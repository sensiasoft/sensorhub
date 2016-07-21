/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence;

import org.junit.Before;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;


public class TestInMemoryBasicStorage extends AbstractTestBasicStorage<InMemoryBasicStorage>
{
    
    @Before
    public void init() throws Exception
    {
        StorageConfig config = new StorageConfig();
        config.name = "In-Memory Storage";
        config.autoStart = true;        
        
        storage = new InMemoryBasicStorage();
        storage.init(config);
    }
    

    @Override
    protected void forceReadBackFromStorage()
    {
            
    }
    
}
