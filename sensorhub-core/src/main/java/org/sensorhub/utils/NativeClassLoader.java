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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashSet;
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
    private HashSet<String> loadedLibraries = new HashSet<String>();
    

    public NativeClassLoader()
    {
        this(NativeClassLoader.class.getClassLoader());
    }


    public NativeClassLoader(ClassLoader parent)
    {
        super((parent instanceof URLClassLoader) ? ((URLClassLoader) parent).getURLs() : EMPTY_URLS, parent);
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


    protected String findLibrary(String libname)
    {
        if (log == null)
            log = LoggerFactory.getLogger(NativeClassLoader.class);
        
        if (loadedLibraries.contains(libname))
            return null;    
        
        String lib = System.mapLibraryName(libname);
        URL url = findResource(lib);
        
        if (url == null)
        {
            url = findResource("lib/native/" + osName() + "/" + osArch() + "/" + lib);
            if (url == null)
                return null;
        }

        loadedLibraries.add(libname);
        return extractResource(url);
    }
    
    
    private String extractResource(URL url)
    {
        try
        {
            URLConnection con = url.openConnection();
            String libPath;
            
            if (con instanceof JarURLConnection)
            {                
                // get resource from jar
                JarURLConnection jar = (JarURLConnection)con;
                InputStream in = new BufferedInputStream(jar.getInputStream());
                
                // copy to temp location (folder named as jar file)
                File jarfile = new File(jar.getJarFile().getName());
                File tmpdir = Files.createTempDirectory(jarfile.getName()).toFile();
                File outfile = new File(tmpdir, jar.getJarEntry().getName());
                outfile.getParentFile().mkdirs();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));            
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0)
                    out.write(buffer, 0, len);
                out.close();
                in.close();
    
                // use path to temp file
                libPath = outfile.getPath();
            }
            else
            {
                // if not in JAR, use filesystem path directly
                libPath = url.getFile();
            }            
            
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
