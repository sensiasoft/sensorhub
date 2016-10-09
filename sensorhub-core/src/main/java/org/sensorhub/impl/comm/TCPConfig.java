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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.ValueRange;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;


/**
 * <p>
 * Driver configuration options for the TCP/IP network protocol
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class TCPConfig extends IPConfig
{    
    @DisplayInfo(desc="Port number to connect to on remote host")
    @ValueRange(min=0, max=65535)
    public int remotePort;
    
    @DisplayInfo(label="User Name", desc="Remote user name")
    public String user;
    
    @DisplayInfo(label="Password", desc="Remote password")
    @FieldType(Type.PASSWORD)
    public String password;
    
    @DisplayInfo(desc="Secure communications with SSL/TLS")
    public boolean enableTLS;
    
}
