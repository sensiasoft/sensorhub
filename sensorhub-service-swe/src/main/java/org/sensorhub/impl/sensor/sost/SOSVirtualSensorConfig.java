/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sost;

import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration for SOS virtual sensors created with InsertSensor
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Mar 2, 2014
 */
public class SOSVirtualSensorConfig extends SensorConfig
{
    private static final long serialVersionUID = -4090502671550227514L;
    
    
    public String sensorUID;
    
    
    public SOSVirtualSensorConfig()
    {
        this.moduleClass = SOSVirtualSensor.class.getCanonicalName();
    }

}
