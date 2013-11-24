/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.vast.ows.sos.ISOSDataProviderFactory;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.sensorML.SMLFeature;
import org.vast.util.DateTime;


public interface IDataProviderFactory extends ISOSDataProviderFactory
{

    /**
     * Allows to check if provider is enabled
     * @return
     */
    public boolean isEnabled();
    
    
    /**
     * Builds the offering capabilities using the provider configuration
     * This will connect to source providers to retrieve the necessary metadata
     * @return SOS capabilities object containing the maximum of metadata
     */
    public SOSOfferingCapabilities generateCapabilities() throws Exception;
        
    
    /**
     * Retrieves the SensorML description associated to this data source
     * @return
     */
    public SMLFeature generateSensorMLDescription(DateTime t) throws Exception;
    
    
    /**
     * Called when the provider is removed
     */    
    public void cleanup();

}