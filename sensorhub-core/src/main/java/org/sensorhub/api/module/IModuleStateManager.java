/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.io.InputStream;
import java.io.OutputStream;


public interface IModuleStateManager
{   
    public String getAsString(String key);
    
    public Float getAsFloat(String key);
    
    public Double getAsDouble(String key);
    
    public Integer getAsInt(String key);

    public Long getAsLong(String key);
    
    public InputStream getAsInputStream(String key);
    
    public String getFolder();
    
    public void put(String key, float value);
    
    public void put(String key, double value);
    
    public void put(String key, int value);
    
    public void put(String key, long value);
    
    public void put(String key, String value);
    
    public OutputStream getOutputStream(String key);
    
    public void flush();
    
    public void cleanup();
}
