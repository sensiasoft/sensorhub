/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sensorthings;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;


/**
 * <p>
 * Configuration class for the SensorThings API module
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jul 15, 2015
 */
public class SensorThingsConfig extends ServiceConfig
{
    @DisplayInfo(label="Observation Storage IDs", desc="List of observation storage to expose through SensorThings")
    public List<String> obsStorageIDs = new ArrayList<String>();
}
