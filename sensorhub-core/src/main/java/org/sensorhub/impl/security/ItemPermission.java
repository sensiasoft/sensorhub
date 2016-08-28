/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security;

import org.sensorhub.api.security.IPermission;


/**
 * <p>
 * Basic named permission used to build hierarchical permission sets 
 * </p>
 *
 * @author Alex Robin
 * @since Aug 22, 2016
 */
public class ItemPermission extends AbstractPermission
{

    public ItemPermission(IPermission parent, String name)
    {
        super(parent, name, null);
    }
    
    
    public ItemPermission(IPermission parent, String name, String errorMsg)
    {
        super(parent, name, errorMsg);
    }
    
}
