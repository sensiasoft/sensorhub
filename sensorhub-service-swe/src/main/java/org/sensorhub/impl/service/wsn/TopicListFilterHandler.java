/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.wsn;

import javax.xml.namespace.QName;
import org.w3c.dom.Element;
import org.apache.muse.ws.notification.Filter;
import org.apache.muse.ws.notification.WsnConstants;
import org.apache.muse.ws.notification.impl.FilterFactoryHandler;
import org.apache.muse.ws.notification.topics.WstConstants;
import org.apache.muse.ws.resource.basefaults.BaseFault;


public class TopicListFilterHandler implements FilterFactoryHandler
{
    
	
	public boolean accepts(QName filterName, String filterDialect)
    {
        boolean rightName = filterName.equals(WsnConstants.TOPIC_EXPRESSION_QNAME);
        boolean rightDialect = filterDialect.equals(WstConstants.FULL_TOPIC_URI);
        return rightName && rightDialect;
    }
    
    
    public Filter newInstance(Element filterXML) throws BaseFault
    {
        String dialect = filterXML.getAttribute(WsnConstants.DIALECT);        
        return new TopicListFilter(filterXML, dialect);
    }
}
