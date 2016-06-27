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

import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Driver configuration options for RS232 hardware interface
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class UARTConfig implements ICommConfig
{
	public enum Parity { PARITY_EVEN, PARITY_MARK, PARITY_NONE, PARITY_ODD, PARITY_SPACE };
	
	@DisplayInfo(desc="Serial port device name. Usually something like /dev/ttyXXX on Linux")
    public String portName;
	
	public int baudRate = 9600;
	
	public byte dataBits = 8;
	
	public byte stopBits = 1;
	
	public Parity parity = Parity.PARITY_NONE;
	
	@DisplayInfo(desc="Timeout after which data is released to the caller if at least one byte was received (in ms)")
	public int receiveTimeout = -1;
	
	@DisplayInfo(desc="Minimum number of bytes to receive before they are sent to the caller")
    public int receiveThreshold = 1;
}
