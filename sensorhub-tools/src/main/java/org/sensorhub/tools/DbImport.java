/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.tools;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.impl.persistence.perst.BasicStorageConfig;
import org.sensorhub.impl.persistence.perst.BasicStorageImpl;
import org.vast.cdm.common.DataStreamParser;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWECommonUtils;
import org.vast.swe.SWEHelper;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class DbImport
{

    public static void main(String[] args) throws Exception
    {
        args = new String[] {
            "/home/alex/Documents/Projects/Workspace_OGC/sensorhub/sensorhub-test/commute_2015-02-16/urn:android:device:060693280a28e015.dat.export.metadata",
            "/home/alex/Documents/Projects/Workspace_OGC/sensorhub/sensorhub-test/commute_2015-02-16/test-db.dat"
        };
        
        if (args.length < 2)
        {
            System.out.println("Usage: DbImport export_file storage_path");
            System.exit(1);
        }
        
        // open storage
        String dbPath = args[1];
        BasicStorageConfig dbConf = new BasicStorageConfig();
        dbConf.enabled = true;
        dbConf.memoryCacheSize = 1024;
        dbConf.storagePath = dbPath;
        BasicStorageImpl db = new BasicStorageImpl();
        db.init(dbConf);
        db.start();
        
        // read XML metadata file
        File metadataFile = new File(args[0]);
        DOMHelper dom = new DOMHelper("file://" + metadataFile.getAbsolutePath(), false);
        SMLUtils smlUtils = new SMLUtils();
        SWECommonUtils sweUtils = new SWECommonUtils();
        
        // import each sensorML description
        NodeList smlElts = dom.getElements(DbConstants.SECTION_SENSORML);
        for (int i = 0; i < smlElts.getLength(); i++)
        {
            Element processElt = dom.getFirstChildElement((Element)smlElts.item(i));
            AbstractProcess process = smlUtils.readProcess(dom, processElt);
            db.storeDataSourceDescription(process);
            System.out.println("Imported SensorML description " + process.getId());
        }
        
        // import data stores
        NodeList dataStoreElts = dom.getElements(DbConstants.SECTION_DATASTORE);
        for (int i = 0; i < dataStoreElts.getLength(); i++)
        {
            Element dataStoreElt = (Element)dataStoreElts.item(i);
            String dataStoreName = dom.getAttributeValue(dataStoreElt, "name");
            Element resultStructElt = dom.getElement(dataStoreElt, "elementType/*");
            DataComponent recordStruct = sweUtils.readComponent(dom, resultStructElt);
            Element resultEncodingElt = dom.getElement(dataStoreElt, "encoding/*");
            DataEncoding recordEncoding = sweUtils.readEncoding(dom, resultEncodingElt);
            ITimeSeriesDataStore<IDataFilter> dataStore = db.addNewDataStore(dataStoreName, recordStruct, recordEncoding);
            System.out.println("Imported data store " + dataStoreName);
            System.out.println("Importing records...");
            
            // read records data
            DataStreamParser recordParser = null;
            try
            {
                File dataFile = new File(metadataFile.getParent(), dataStoreName + ".export.data");
                InputStream recordInput = new BufferedInputStream(new FileInputStream(dataFile));
                DataInputStream dis = new DataInputStream(recordInput);
                
                // prepare record writer
                recordParser = SWEHelper.createDataParser(recordEncoding);
                recordParser.setDataComponents(recordStruct);
                recordParser.setInput(recordInput);
                
                // write all records
                int recordCount = 0;
                while (true)
                {
                    try
                    {
                        double timeStamp = dis.readDouble();
                        String producerID = dis.readUTF();
                        if (producerID.equals(DbConstants.KEY_NULL_PRODUCER))
                            producerID = null;
                            
                        DataKey key = new DataKey(producerID, timeStamp);
                        DataBlock dataBlk = recordParser.parseNextBlock();
                        dataStore.store(key, dataBlk);
                        recordCount++;
                    }
                    catch (EOFException e)
                    {
                        break;
                    }
                }
                
                System.out.println("Imported " + recordCount + " records");
            }
            finally
            {
                if (recordParser != null)
                    recordParser.close();                
            }
        }
    }
}
