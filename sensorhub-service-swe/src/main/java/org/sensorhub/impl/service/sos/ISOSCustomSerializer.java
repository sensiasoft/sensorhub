/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.io.IOException;
import org.vast.ows.OWSRequest;


/**
 * <p>
 * Interface for all custom serializers of GetResult response.<br/>
 * For instance, this is used for generating multipart MJPEG or muxing H264
 * to MP4 on the fly so that video data can be streamed directly to a browser.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 5, 2016
 */
public interface ISOSCustomSerializer
{
    public void write(ISOSDataProvider dataProvider, OWSRequest request) throws IOException;
}
