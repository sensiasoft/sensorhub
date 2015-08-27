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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


/**
 * <p>
 * Static utility methods for dealing with files
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class FileUtils
{
    
    
    /**
     * Creates a valid file name from the given string
     * @param name
     * @return a valid file name
     */
    public static String safeFileName(String name)
    {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    
    /**
     * Recursively deletes a folder and all its content
     * @param folder
     * @throws IOException
     */
    public static void deleteRecursively(File folder) throws IOException
    {
        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException
            {
                if (e == null)
                {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                else
                {
                    throw e;
                }
            }

            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes att) throws IOException
            {
                Files.delete(f);
                return FileVisitResult.CONTINUE;
            }
            
        });
    }

}
