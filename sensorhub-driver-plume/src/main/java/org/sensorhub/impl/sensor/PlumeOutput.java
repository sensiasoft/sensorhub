/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataRecordImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * </p>
 *
 * @author Tony Cook
 * @since Sep 22, 2015
 */
public class PlumeOutput extends AbstractSensorOutput<PlumeSensor>
{
    private static final Logger log = LoggerFactory.getLogger(PlumeOutput.class);
    DataComponent plumeStruct;
	BinaryEncoding encoding;


    public PlumeOutput(PlumeSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "plumeModel";
    }


    @Override
    protected void init()
    {
    	/**
    		- sourceLat,sourceLon,sourceAlt

    		- time0, numPoints0
    		- x00,y00,z00
    		- x01,y01,z01
    		...
    		- x0N-1,y0N-1,z0N-1
    		- time1, numPoints1
    		- x10,y10,z10
    		- x11,y11,z11
    		*/
        SWEHelper fac = new SWEHelper();
        plumeStruct = new DataRecordImpl(2);
        plumeStruct.setName(getName());
        plumeStruct.setDefinition("http://sensorml.com/ont/swe/property/LagrangianPlumeModel");
        
        // Location
        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("PlumeSourceLocation");
        locVector.setDescription("Location of plume source");
        plumeStruct.addComponent("location", locVector);

        // Point data
        DataRecord plumeStep = fac.newDataRecord(3);
        plumeStep.setDescription("plumeStep represents a single timeStep with point data");
//        plumeStep.setDefinition(definition);
        
        // time
        plumeStep.addComponent("time",  fac.newTimeStampIsoUTC());
        // numParticles
        Count numPoints = fac.newCount();
        numPoints.setId("NUM_POINTS");
        plumeStep.addComponent("num_pos", numPoints);   
        // and the actual particle data
        DataArray ptArr = fac.newDataArray();
        ptArr.setElementType("point", fac.newLocationVectorLLA(""));
        ptArr.setElementCount(numPoints);
        plumeStep.addComponent("points", ptArr);
        
        // Define Array of steps
        DataArray stepArr = fac.newDataArray();
        stepArr.addComponent("plumeStep", plumeStep);
        
        // and add array to plumeStruct
        plumeStruct.addComponent("plumeStepArray", stepArr);
        
		encoding = SWEHelper.getDefaultBinaryEncoding(plumeStruct);
    }


    protected void start()
    {
    }


    protected void stop()
    {
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 600.0;  // need to set this based on value in output file
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return plumeStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }

}
