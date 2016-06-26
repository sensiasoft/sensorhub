/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.impl.module.RobustConnectionConfig;


public class RobustIPConnectionConfig extends RobustConnectionConfig
{

    @DisplayInfo(desc="Enable to check connectivity to remote host using ping before attempting further operations. "
            + "Disable this option if you know remote host is not responding to ping requests")
    public boolean testPing = true;
}
