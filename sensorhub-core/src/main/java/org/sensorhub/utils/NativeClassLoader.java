/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Custom class loader for finding native libraries anywhere on the
 * classpath.<br/>
 * Native libraries must be stored in a folder named "lib/native/{os.name}/{os.arch}/"
 * (e.g. lib/native/linux/x86_64/libsomething.so)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 3, 2015
 */
public class NativeClassLoader extends URLClassLoader
{
    static final URL[] EMPTY_URLS = new URL[] {};
    private Logger log; // cannot be a static logger in case it is used as system classloader
    private Map<String, String> loadedLibraries = new HashMap<String, String>();
    private List<File> tmpFolders = new ArrayList<File>(); 
    

    public NativeClassLoader()
    {
        this(NativeClassLoader.class.getClassLoader());
    }


    public NativeClassLoader(ClassLoader parent)
    {
        super((parent instanceof URLClassLoader) ? ((URLClassLoader) parent).getURLs() : EMPTY_URLS, parent);
        setupShutdownHook();
    }
    
    
    protected void setupShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run()
            {
                // remove temp folders
                for (File f: tmpFolders)
                {
                    try
                    {
                        FileUtils.deleteRecursively(f);
                    }
                    catch (IOException e)
                    {
                    }
                }
            }            
        });
    }


    static String osName()
    {
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Linux"))
            return "linux";
        if (osname.startsWith("Mac OS X"))
            return "macosx";
        if (osname.startsWith("SunOS"))
            return "solaris";
        if (osname.startsWith("Windows"))
            return "windows";
        return "unknown";
    }


    static String osArch()
    {
        String osarch = System.getProperty("os.arch");
        
        if (osarch.equals("amd64"))
            return "x86_64";
        
        if (osarch.equals("i386"))
            return "x86";
        
        if (osarch.equals("arm"))
        {
            String armabi = System.getProperty("os.armabi");
            if (armabi == null)
                armabi = "v6_hf";
            return osarch + armabi;
        }
        
        return osarch;
    }


    @Override
    protected String findLibrary(String libName)
    {
        // setup logger cause it doesn't work the static way when used as system class loader
        if (log == null)
            log = LoggerFactory.getLogger(NativeClassLoader.class);
        
        // get path directly if we have already found this library
        String libPath = loadedLibraries.get(libName);
        
        // first try with specific OS name
        if (libPath == null)
        {
            String libFileName = System.mapLibraryName(libName);
            libPath = findLibraryFile(libName, libFileName);
        }
        
        // otherwise try directly with library name
        if (libPath == null)
            libPath = findLibraryFile(libName, libName);
        
        return libPath;
    }
    
    
    protected String findLibraryFile(String libName, String libFileName)
    {
        // try to get it from embedded native lib folder
        URL url = findResource("lib/native/" + osName() + "/" + osArch() + "/" + libFileName);
        
        // if we have nothing return null to let VM search for it in java.library.path
        if (url == null)
            return null;
        
        return extractResource(libName, url);
    }
    
    
    private String extractResource(String libName, URL url)
    {
        try
        {
            URLConnection con = url.openConnection();
            String libPath;
            
            if (con instanceof JarURLConnection)
            {                
                // get resource from jar
                JarURLConnection jarItemConn = (JarURLConnection)con;
                InputStream in = new BufferedInputStream(jarItemConn.getInputStream());
                
                // copy to temp location (folder named as jar file)
                File jarFile = new File(jarItemConn.getJarFile().getName());
                File tmpDir = Files.createTempDirectory(jarFile.getName()).toFile();
                tmpFolders.add(tmpDir);
                File outFile = new File(tmpDir, jarItemConn.getJarEntry().getName());
                outFile.getParentFile().mkdirs();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));            
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0)
                    out.write(buffer, 0, len);
                out.close();
                in.close();
    
                // use path to temp file
                libPath = outFile.getPath();
            }
            else
            {
                // if not in JAR, use filesystem path directly
                libPath = url.getFile();
            }            
            
            loadedLibraries.put(libName, libPath);
            log.debug("Using native library from: " + libPath);
            return libPath;
        }
        catch (java.io.IOException e)
        {
            return null;
        }
    }


    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> c = findLoadedClass(name);
        
        if (c == null)
        {
            try
            {
                c = getParent().getParent().loadClass(name);
            }
            catch (ClassNotFoundException e)
            {
                c = findClass(name);
            }
        }

        if (resolve)
        {
            resolveClass(c);
        }
        
        return c;
    }
}
