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

import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.IDeviceInfo;
import org.sensorhub.api.comm.IDeviceScanCallback;
import org.sensorhub.api.comm.INetworkInfo;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Button.ClickEvent;


/**
 * <p>
 * Admin panel for networking modules.<br/>
 * This adds features to view available networks and scan for devices.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 1.0
 */
public class NetworkAdminPanel extends DefaultModulePanel<ICommNetwork<?>> implements IModuleAdminPanel<ICommNetwork<?>>
{
    private static final long serialVersionUID = -183320020448726954L;

    boolean scanning;
    Button scanButton;
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ICommNetwork<?> module)
    {
        super.build(beanItem, module);       
        
        // add sub form
        final GridLayout form = new GridLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        form.setSpacing(true);
        
        // add sections
        addAvailableNetworks(form, module);
        addScannedDevices(form, module);
        
        addComponent(form);
    }
    
    
    protected void addAvailableNetworks(final GridLayout form, final ICommNetwork<?> module)
    {
        // section title
        Label sectionLabel = new Label("Available Networks");
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        form.addComponent(sectionLabel);
        
        // network table
        final Table table = new Table();
        table.setWidth(100.0f, Unit.PERCENTAGE);
        table.setPageLength(3);
        table.setSelectable(true);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.addContainerProperty("Network Type", String.class, null);
        table.addContainerProperty("Interface Name", String.class, null);
        table.addContainerProperty("Hardware Address", String.class, null);
        table.addContainerProperty("Logical Address", String.class, null);

        int i = 0;
        for (INetworkInfo netInfo: module.getAvailableNetworks())
        {
            table.addItem(new Object[] {
                    netInfo.getNetworkType().toString(),
                    netInfo.getInterfaceName(),
                    netInfo.getHardwareAddress(),
                    netInfo.getLogicalAddress()}, i);
            i++;
        }
        
        form.addComponent(table);
    }
    
    
    @SuppressWarnings("serial")
    protected void addScannedDevices(final GridLayout form, final ICommNetwork<?> module)
    {
        // section title
        Label sectionLabel = new Label("Detected Devices");
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        form.addComponent(sectionLabel);
        
        // scan button
        scanButton = new Button("Start Scan");
        scanButton.setIcon(REFRESH_ICON);
        scanButton.addStyleName("scan-button");
        scanButton.setEnabled(module.isEnabled());
        form.addComponent(scanButton);
        
        // device table
        final Table table = new Table();
        table.setWidth(100.0f, Unit.PERCENTAGE);
        table.setPageLength(10);
        table.setSelectable(true);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.addContainerProperty("Name", String.class, null);
        table.addContainerProperty("Type", String.class, null);
        table.addContainerProperty("Address", String.class, null);
        table.addContainerProperty("Signal Level", String.class, null);

        // scan button handler
        scanButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    if (!scanning)
                    {
                        scanning = true;
                        scanButton.setCaption("Stop Scan");
                        table.clear();
                        
                        module.getDeviceScanner().startScan(new IDeviceScanCallback(){
                            @Override
                            public void onDeviceFound(final IDeviceInfo info)
                            {
                                AdminUI.getInstance().access(new Runnable() {
                                    @Override
                                    public void run() {
                                        String itemId = info.getAddress() + '/' + info.getType();
                                        // if address was already detected, refresh info
                                        if (table.containsId(itemId))
                                        {
                                            table.getContainerProperty(itemId, "Name").setValue(info.getName());
                                            table.getContainerProperty(itemId, "Type").setValue(info.getType());
                                            table.getContainerProperty(itemId, "Signal Level").setValue(info.getSignalLevel());
                                        }
                                        else
                                        {
                                            table.addItem(new Object[] {
                                                    info.getName(),
                                                    info.getType(),
                                                    info.getAddress(),
                                                    info.getSignalLevel()
                                                }, itemId);
                                        }
                                        AdminUI.getInstance().push();
                                    }
                                });                                         
                            }
    
                            @Override
                            public void onScanError(Throwable e)
                            {
                                String msg = "Error during device scan";
                                Page page = AdminUI.getInstance().getPage();
                                new Notification("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE).show(page);
                                AdminUI.log.error(msg, e);               
                            }                        
                        });
                        
                        // automatically stop scan after 30s
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run()
                            {
                                stopScan(module);
                            }                        
                        }, 30000);
                    }
                    else
                    {
                        stopScan(module);
                    }
                }
                catch (Exception e)
                {
                    String msg = "Error scanning for devices";
                    Page page = AdminUI.getInstance().getPage();
                    new Notification("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE).show(page);
                    //Notification.show("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                    AdminUI.log.error(msg, e);
                }
            }
        });        
        
        form.addComponent(table);
    }
    
    
    protected void stopScan(ICommNetwork<?> module)
    {
        module.getDeviceScanner().stopScan();
        scanning = false;
        
        AdminUI.getInstance().access(new Runnable() {
            @Override
            public void run() {
                scanButton.setCaption("Start Scan");
                AdminUI.getInstance().push();
            }
        });
        
    }
}
