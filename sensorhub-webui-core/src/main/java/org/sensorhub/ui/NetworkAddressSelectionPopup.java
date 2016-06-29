/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.Collection;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.ICommNetwork.NetworkType;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.ui.NetworkAdminPanel.NetworkScanPanel;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


public class NetworkAddressSelectionPopup extends Window
{
    private static final long serialVersionUID = -7139125123797297769L;
    
    
    protected interface AddressSelectionCallback
    {
        public void onSelected(String address);
    }
    
    
    @SuppressWarnings({ "serial" })
    public NetworkAddressSelectionPopup(final NetworkType addressType, final AddressSelectionCallback callback)
    {
        super("Select Network Host");
        
        setWidth(60.f, Unit.PERCENTAGE);
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        
        // create network + address selection panel
        Collection<ICommNetwork<?>> networks = SensorHub.getInstance().getNetworkManager().getLoadedModules(addressType);
        ICommNetwork<?> network = networks.iterator().next();
        final NetworkScanPanel scanPanel = new NetworkScanPanel(network);
        scanPanel.setMargin(false);
        layout.addComponent(scanPanel);
        
        // buttons
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        layout.addComponent(buttons);
        layout.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);
        
        // add useAddress button
        Button okAddressButton = new Button("Use Address");
        okAddressButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event)
            {
                notifyItemSelected(scanPanel, callback, false);
            }
        });
        buttons.addComponent(okAddressButton);
        
        // add useName button
        Button okNameButton = new Button("Use Name");
        okNameButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event)
            {
                notifyItemSelected(scanPanel, callback, true);
            }
        });
        buttons.addComponent(okNameButton);
        
        setContent(layout);
        center();
    }
    
    
    protected void notifyItemSelected(NetworkScanPanel scanPanel, AddressSelectionCallback callback, boolean useName)
    {
        IDeviceInfo selectedDevice = scanPanel.getSelectedDevice();
        
        if (selectedDevice != null)
        {
            if (useName)
                callback.onSelected(selectedDevice.getName());
            else
                callback.onSelected(selectedDevice.getAddress());
        }
        
        close();
    }
}
