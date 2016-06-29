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
import com.vaadin.data.Item;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.UI;


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
    private static final String PROP_NAME = "Name";
    private static final String PROP_TYPE = "Type";
    private static final String PROP_ADDRESS = "Address";
    private static final String PROP_SIG_LEVEL = "Signal Level";
    private static final String DEV_INFO_OBJ = "DEV_OBJ";
    
    
    public static class NetworkScanPanel extends GridLayout
    {
        private static final long serialVersionUID = -1587883711970632124L;
        ICommNetwork<?> module;
        Button scanButton;
        Table deviceTable;
        Timer stopTimer =  new Timer();
        TimerTask timerTask;
        
        
        public NetworkScanPanel(final ICommNetwork<?> module)
        {
            this.module = module;
            
            setWidth(100.0f, Unit.PERCENTAGE);
            setSpacing(true);
            this.setColumns(1);
            
            addAvailableNetworks();
            addScannedDevicesTable();
        }
        
        
        protected void addAvailableNetworks()
        {
            // section title
            Label sectionLabel = new Label("Available Networks");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            addComponent(sectionLabel);
            
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
            
            addComponent(table);
        }
        
        
        @SuppressWarnings("serial")
        protected void addScannedDevicesTable()
        {
            // section title
            Label sectionLabel = new Label("Detected Devices");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            addComponent(sectionLabel);
            
            // scan button
            scanButton = new Button("Start Scan");
            scanButton.setIcon(REFRESH_ICON);
            scanButton.addStyleName("scan-button");
            scanButton.setEnabled(module.isStarted());
            addComponent(scanButton);
            
            // device table
            deviceTable = new Table();
            deviceTable.setWidth(100.0f, Unit.PERCENTAGE);
            deviceTable.setPageLength(10);
            deviceTable.setSelectable(true);
            deviceTable.setImmediate(true);
            deviceTable.setColumnReorderingAllowed(false);
            deviceTable.addContainerProperty(PROP_NAME, String.class, null);
            deviceTable.addContainerProperty(PROP_TYPE, String.class, null);
            deviceTable.addContainerProperty(PROP_ADDRESS, String.class, null);
            deviceTable.addContainerProperty(PROP_SIG_LEVEL, String.class, null);
            deviceTable.addContainerProperty(DEV_INFO_OBJ, IDeviceInfo.class, null);
            deviceTable.setVisibleColumns(PROP_NAME, PROP_TYPE, PROP_ADDRESS, PROP_SIG_LEVEL);
            
            // scan button handler
            scanButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(ClickEvent event)
                {
                    if (!module.getDeviceScanner().isScanning())
                    {
                        scanButton.setCaption("Stop Scan");
                        deviceTable.removeAllItems();
                        
                        new Thread() {
                            public void run()
                            {
                                module.getDeviceScanner().startScan(new IDeviceScanCallback(){
                                    @Override
                                    public void onDeviceFound(final IDeviceInfo info)
                                    {
                                        final UI ui = NetworkScanPanel.this.getUI();
                                        if (ui != null)
                                        {
                                            ui.access(new Runnable() {
                                                public void run() {
                                                    String itemId = info.getAddress() + '/' + info.getType();
                                                    Item item;
                                                    
                                                    // create or refresh device info
                                                    if (!deviceTable.containsId(itemId))
                                                    {
                                                        item = deviceTable.addItem(itemId);
                                                        item.getItemProperty(DEV_INFO_OBJ).setValue(info); 
                                                    }
                                                    else
                                                        item = deviceTable.getItem(itemId);
                                                                                            
                                                    item.getItemProperty(PROP_NAME).setValue(info.getName());
                                                    item.getItemProperty(PROP_TYPE).setValue(info.getType());
                                                    item.getItemProperty(PROP_ADDRESS).setValue(info.getAddress());
                                                    item.getItemProperty(PROP_SIG_LEVEL).setValue(info.getSignalLevel());                                        
                                                    
                                                    ui.push();
                                                }
                                            });
                                        }
                                    }
            
                                    @Override
                                    public void onScanError(final Throwable e)
                                    {
                                        final String msg = "Error during device scan";
                                        
                                        final UI ui = NetworkScanPanel.this.getUI();
                                        if (ui != null)
                                        {
                                            ui.access(new Runnable() {
                                                public void run() {
                                                    
                                                    new Notification("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE).show(ui.getPage());
                                                }
                                            });
                                        }              
                                    }                        
                                });
                            }
                        }.start();                            
                        
                        // automatically stop scan after 30s
                        timerTask = new TimerTask() {
                            @Override
                            public void run()
                            {
                                stopScan();
                            }                        
                        };
                        stopTimer.schedule(timerTask, 30000);
                    }
                    else
                    {
                        stopScan();
                    }
                }
            });        
            
            addComponent(deviceTable);
        }
        
        
        public void stopScan()
        {
            if (timerTask != null)
                timerTask.cancel();
            
            module.getDeviceScanner().stopScan();
            
            final UI ui = NetworkScanPanel.this.getUI();
            if (ui != null)
            {
                ui.access(new Runnable() {
                    @Override
                    public void run() {
                        scanButton.setCaption("Start Scan");
                        ui.push();
                    }
                });
            }
        }
        
        
        public IDeviceInfo getSelectedDevice()
        {
            Object selectedItemId = deviceTable.getValue();
            if (selectedItemId != null)
            {
                final Item item = deviceTable.getItem(selectedItemId);
                return (IDeviceInfo)item.getItemProperty(DEV_INFO_OBJ).getValue();
            }
            
            return null;
        }
        
        
        @Override
        public void detach()
        {
            stopScan();
        }
    }
    
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ICommNetwork<?> module)
    {
        super.build(beanItem, module);       
        
        // add scan panel
        NetworkScanPanel scanPanel = new NetworkScanPanel(module);
        scanPanel.setMargin(false);
        addComponent(scanPanel);
    }
}
