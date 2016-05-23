/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.utils;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Test;
import org.sensorhub.utils.FileUtils;


public class TestFileUtils
{

    @Test
    public void testFixFileName()
    {
        String in;
        String expected = "my_file_2.dat";
        String msg1 = "File name is unsafe";
        String msg2 = "Incorrectly generated file name";
        
        in = "my_file2.dat";
        assertTrue("File name is safe", FileUtils.isSafeFileName(in));
        
        in = "my_file#2.dat";
        assertFalse(msg1, FileUtils.isSafeFileName(in));
        assertEquals(msg2, expected, FileUtils.safeFileName(in));
        
        in = "my:file#2.dat";
        assertFalse(msg1, FileUtils.isSafeFileName(in));
        assertEquals(msg2, expected, FileUtils.safeFileName(in));
        
        in = "my$$file##2.dat";
        assertFalse(msg1, FileUtils.isSafeFileName(in));
        assertEquals(msg2, expected, FileUtils.safeFileName(in));
    }
    
    
    @Test
    public void testCheckFilePath()
    {
        String in;
        char sep = File.separatorChar;
        String msg1 = "File path is valid";
        String msg2 = "File path is invalid";
        
        in = sep + "folder" + sep + "my_file2.dat";
        assertTrue(msg1, FileUtils.isSafeFilePath(in));
        
        in = "c:" + sep + "folder" + sep + "my_file2.dat";
        assertTrue(msg1, FileUtils.isSafeFilePath(in));
        
        in = ":c:" + sep + "folder" + sep + "my_file2.dat";
        assertFalse(msg1, FileUtils.isSafeFilePath(in));
        
        in = "/drive/folder/my_file#2.dat";
        assertFalse(msg2, FileUtils.isSafeFilePath(in));
    }

}
