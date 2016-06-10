/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.garret.perst.IFile;
import org.garret.perst.StorageError;
import sun.nio.ch.FileChannelImpl;
import sun.misc.Unsafe;


@SuppressWarnings("restriction")
public class MappedFileUnsafe implements IFile
{
    private static final Unsafe unsafe;
    private static final Method mmap;
    private static final Method unmmap;
    private static final int BYTE_ARRAY_OFFSET;

    private RandomAccessFile file;
    private FileChannel chan;
    private FileLock fileLock;
    private long addr, mapSize;
    private final String filePath;

    static
    {
        try
        {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);

            mmap = getMethod(FileChannelImpl.class, "map0", int.class, long.class, long.class);
            unmmap = getMethod(FileChannelImpl.class, "unmap0", long.class, long.class);

            BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    //Bundle reflection calls to get access to the given method
    private static Method getMethod(Class<?> cls, String name, Class<?>... params) throws Exception
    {
        Method m = cls.getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }


    //Round to next 4096 bytes
    private static long roundTo4096(long i)
    {
        return (i + 0xfffL) & ~0xfffL;
    }


    //Given that the location and size have been set, map that location
    //for the given length and set this.addr to the returned offset
    private void mapAndSetOffset() throws Exception
    {
        file = new RandomAccessFile(filePath, "rw");
        file.setLength(this.mapSize);

        chan = file.getChannel();
        this.addr = (long) mmap.invoke(chan, 1, 0L, this.mapSize);
    }


    public MappedFileUnsafe(final String filePath, long len) throws Exception
    {
        this.filePath = filePath;
        this.mapSize = roundTo4096(len);
        mapAndSetOffset();
    }


    //Callers should synchronize to avoid calls in the middle of this, but
    //it is undesirable to synchronize w/ all access methods.
    public void remap(long nLen) throws Exception
    {
        unmmap.invoke(chan, addr, mapSize);
        this.mapSize = roundTo4096(nLen);
        mapAndSetOffset();
    }


    public int getInt(long pos)
    {
        return unsafe.getInt(pos + addr);
    }


    public long getLong(long pos)
    {
        return unsafe.getLong(pos + addr);
    }


    public void putInt(long pos, int val)
    {
        unsafe.putInt(pos + addr, val);
    }


    public void putLong(long pos, long val)
    {
        unsafe.putLong(pos + addr, val);
    }


    @Override
    public void close()
    {
        try
        {
            chan.close();
        }
        catch (IOException x)
        {
            throw new StorageError(StorageError.FILE_ACCESS_ERROR, x);
        }
    }


    @Override
    public long length()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int read(long pos, byte[] data)
    {
        unsafe.copyMemory(null, pos + addr, data, BYTE_ARRAY_OFFSET, data.length);
        return data.length;
    }


    @Override
    public void write(long pos, byte[] data)
    {
        unsafe.copyMemory(data, BYTE_ARRAY_OFFSET, null, pos + addr, data.length);
    }


    @Override
    public void sync()
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public boolean tryLock(boolean shared)
    {
        try
        {
            fileLock = chan.tryLock(0, Long.MAX_VALUE, shared);
            return fileLock != null;
        }
        catch (IOException x)
        {
            return true;
        }
    }


    @Override
    public void lock(boolean shared)
    {
        try
        {
            fileLock = chan.lock(0, Long.MAX_VALUE, shared);
        }
        catch (IOException x)
        {
            throw new StorageError(StorageError.LOCK_FAILED, x);
        }
    }


    @Override
    public void unlock()
    {
        try
        {
            fileLock.release();
        }
        catch (IOException x)
        {
            throw new StorageError(StorageError.LOCK_FAILED, x);
        }
    }
}