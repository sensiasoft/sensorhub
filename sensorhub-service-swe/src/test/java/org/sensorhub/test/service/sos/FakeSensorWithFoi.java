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

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import org.sensorhub.test.sensor.FakeSensor;
import org.vast.ogc.gml.GenericFeatureImpl;


public class FakeSensorWithFoi extends FakeSensor
{
    static int MAX_FOIS = 3;
    static String UID_PREFIX = "urn:blabla:myfois:";
    GMLFactory gmlFac = new GMLFactory(true);
    List<AbstractFeature> fois;
    
    
    public FakeSensorWithFoi()
    {
        fois = new ArrayList<AbstractFeature>();
        
        for (int foiNum = 1; foiNum <= MAX_FOIS; foiNum++)
        {
            QName fType = new QName("http://myNsUri", "MyFeature");
            AbstractFeature foi = new GenericFeatureImpl(fType);
            foi.setId("F" + foiNum);
            foi.setUniqueIdentifier(UID_PREFIX + foiNum);
            foi.setName("FOI" + foiNum);
            foi.setDescription("This is feature of interest #" + foiNum);                        
            Point p = gmlFac.newPoint();
            p.setPos(new double[] {foiNum, foiNum, 0.0});
            foi.setLocation(p);
            fois.add(foi);
        }        
    }
    
    
    @Override
    public List<AbstractFeature> getFeaturesOfInterest()
    {
        return fois;
    }
}
