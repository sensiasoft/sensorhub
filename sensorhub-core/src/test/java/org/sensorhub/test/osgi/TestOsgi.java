/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.osgi;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;


public class TestOsgi
{
    private static String CACHE_FOLDER = "osgi-cache";
    
    
    protected Framework getFramework()
    {
        Iterator<FrameworkFactory> it = ServiceLoader.load(org.osgi.framework.launch.FrameworkFactory.class).iterator();
        assertTrue("No OSGI implementation found in classpath", it.hasNext());
        
        Map<String,String> osgiConfig = new HashMap<String,String>();
        //osgiConfig.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERY, "");
        osgiConfig.put("org.osgi.framework.storage", CACHE_FOLDER);
        osgiConfig.put("org.osgi.framework.storage.clean", "onFirstInit");
        Framework fw = it.next().newFramework(osgiConfig);
        
        return fw;
    }
    
    
    protected Bundle installBundle(Framework fw, String bundleURL, String bundleName) throws Exception
    {
        int numBundles = fw.getBundleContext().getBundles().length;
        
        // install bundle
        Bundle newBundle = fw.getBundleContext().installBundle(bundleURL);
        assertTrue("bundle should not be null", newBundle != null);        
        System.out.println("Bundle name is " + newBundle.getSymbolicName());
        
        // check that it was properly loaded
        assertEquals("Wrong number of loaded bundles", numBundles+1, fw.getBundleContext().getBundles().length);
        assertEquals("Unexpected bundle name", bundleName, newBundle.getSymbolicName());
        assertEquals("Bundle should be in INSTALLED state", Bundle.INSTALLED, newBundle.getState());
        
        return newBundle;
    }
    
    
    @Test
    public void test1StartStopFramework() throws Exception
    {
        Framework fw = getFramework();
        fw.start();
        Thread.sleep(500);
        fw.stop();
        fw.waitForStop(0);
    }
    
    
    @Test
    public void test2InstallBundle() throws Exception
    {
        Framework fw = getFramework();
        fw.start();
        
        // install bundle w/o dependency
        Bundle newBundle = installBundle(fw, getClass().getResource("/test-nodep.jar").toString(), "org.sensorhub.test");
                
        // attempt to start it
        newBundle.start();
        assertEquals("Bundle should be in ACTIVE state", Bundle.ACTIVE, newBundle.getState());
        
        fw.stop();
        fw.waitForStop(0);
    }
    
    
    @Test
    public void test3BundleDependencies() throws Exception
    {
        Framework fw = getFramework();
        fw.start();
        
        assertEquals("Wrong number of loaded bundles", 1, fw.getBundleContext().getBundles().length);
        
        // install 1st bundle
        installBundle(fw, getClass().getResource("/test-nodep.jar").toString(), "org.sensorhub.test");
        
        // install 2nd bundle
        Bundle bundle2 = installBundle(fw, getClass().getResource("/test-withdep.jar").toString(), "org.sensorhub.test2");
        
        bundle2.start();
        assertEquals("Bundle " + bundle2.getSymbolicName() + " should be in ACTIVE state", Bundle.ACTIVE, bundle2.getState());
        
        fw.stop();
        fw.waitForStop(0);
    }
    
    
    @AfterClass
    public static void cleanup()
    {
        try
        {
            FileUtils.deleteDirectory(new File(CACHE_FOLDER));
        }
        catch (IOException e)
        {
        }
    }
}
