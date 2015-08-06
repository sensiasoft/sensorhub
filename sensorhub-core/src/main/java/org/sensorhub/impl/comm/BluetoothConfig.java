/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import org.sensorhub.api.comm.CommConfig;


/**
 * <p>
 * Driver configuration options for the Bluetooth hardware interface
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since June 18, 2015
 */
public class BluetoothConfig extends CommConfig
{	
    public String deviceName;
    public String serviceUuid;
}
