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

import java.util.ArrayList;
import javax.xml.namespace.QName;
import org.apache.muse.util.xml.XmlUtils;
import org.apache.muse.ws.notification.Filter;
import org.apache.muse.ws.notification.NotificationMessage;
import org.apache.muse.ws.notification.WsnConstants;
import org.apache.muse.ws.notification.faults.InvalidTopicExpressionFault;
import org.apache.muse.ws.notification.faults.TopicExpressionDialectUnknownFault;
import org.apache.muse.ws.notification.impl.TopicFilter;
import org.apache.muse.ws.notification.topics.WstConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class TopicListFilter implements Filter
{
	protected ArrayList<TopicFilter> topicFilters;


	public TopicListFilter()
	{
		topicFilters = new ArrayList<TopicFilter>();
	}
	
	
	public TopicListFilter(Element topicExpElt, String dialect)
		throws TopicExpressionDialectUnknownFault, InvalidTopicExpressionFault
	{
		this();
		String[] topicList =  XmlUtils.extractText(topicExpElt).split("\\|");
		
		for (String topic: topicList)
		{
			QName topicName = XmlUtils.parseQName(topic, topicExpElt);
			addTopic(topicName);
		}
	}
	
	
	public boolean accepts(NotificationMessage message)
	{
		// check that at least one topic matches
		for (TopicFilter filter: topicFilters)
			if (filter.accepts(message))
				return true;
		
		return false;
	}


	public Element toXML()
    {
        return toXML(XmlUtils.EMPTY_DOC);
    }
	

    public Element toXML(Document doc)
    {
        Element filterElt = XmlUtils.createElement(doc, WsnConstants.FILTER_QNAME);
        Element topicElt = XmlUtils.createElement(doc, WsnConstants.TOPIC_EXPRESSION_QNAME);
        topicElt.setAttribute(WsnConstants.DIALECT, WstConstants.FULL_TOPIC_URI);
        
        StringBuffer buf = new StringBuffer();
        for (TopicFilter filter: topicFilters)
        {
        	QName topic = filter.getTopic();
        	buf.append(topic.getPrefix() + ":" + topic.getLocalPart() + "|");
        	XmlUtils.setNamespaceAttribute(topicElt, topic.getPrefix(), topic.getNamespaceURI());
        }
        
        String topicList = buf.toString().substring(0, buf.length()-1);
        topicElt.setTextContent(topicList);
        filterElt.appendChild(topicElt);
        
        return filterElt;
    }
	
	
	public void addTopic(QName topicName)
		throws TopicExpressionDialectUnknownFault, InvalidTopicExpressionFault
	{
		topicFilters.add(new TopicFilter(topicName));
	}

}
