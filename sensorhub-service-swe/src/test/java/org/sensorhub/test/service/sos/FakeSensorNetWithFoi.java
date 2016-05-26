/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalComponent;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.test.sensor.FakeSensor;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.sensorML.SMLHelper;


public class FakeSensorNetWithFoi extends FakeSensor implements IMultiSourceDataProducer
{
    static int MAX_FOIS = 3;
    static String FOI_UID_PREFIX = "urn:blabla:myfois:";
    static String SENSOR_UID_PREFIX = "urn:blabla:sensors:";
    GMLFactory gmlFac = new GMLFactory(true);
    Map<String, AbstractFeature> fois;
    Set<String> foiIDs;
    
    
    public FakeSensorNetWithFoi()
    {
        fois = new LinkedHashMap<String, AbstractFeature>();
        foiIDs = new LinkedHashSet<String>();
        
        for (int foiNum = 1; foiNum <= MAX_FOIS; foiNum++)
        {
            QName fType = new QName("http://myNsUri", "MyFeature");
            AbstractFeature foi = new GenericFeatureImpl(fType);
            foi.setId("F" + foiNum);
            foi.setUniqueIdentifier(FOI_UID_PREFIX + foiNum);
            foi.setName("FOI" + foiNum);
            foi.setDescription("This is feature of interest #" + foiNum);                        
            Point p = gmlFac.newPoint();
            p.setPos(new double[] {foiNum, foiNum, 0.0});
            foi.setLocation(p);
            fois.put(SENSOR_UID_PREFIX + foiNum, foi);
            foiIDs.add(foi.getUniqueIdentifier());
        }        
    }
    
    
    @Override
    public Collection<String> getEntityIDs()
    {
        return fois.keySet();
    }


    @Override
    public AbstractProcess getCurrentDescription(String entityID)
    {
        SMLHelper fac = new SMLHelper();
        PhysicalComponent sensor = fac.newPhysicalComponent();
        sensor.setUniqueIdentifier(entityID);
        sensor.setName("Networked sensor " + entityID.substring(entityID.lastIndexOf(':')+1));
        return sensor;
    }


    @Override
    public double getLastDescriptionUpdate(String entityID)
    {
        return 0;
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest(String entityID)
    {
        return fois.get(entityID);
    }


    @Override
    public Collection<AbstractFeature> getFeaturesOfInterest()
    {
        return Collections.unmodifiableCollection(fois.values());
    }


    @Override
    public Collection<String> getFeaturesOfInterestIDs()
    {
        return Collections.unmodifiableCollection(foiIDs);
    }
}
