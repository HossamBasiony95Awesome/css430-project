/*
 * @file    Directory.java
 * @brief   This class is the directory structure for a simple filesystem. It
 *           holds the names of every file, indexed by their inode numbers.
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 14, 2012
 */
public class Directory {
    private static int maxChars = 30;   // max characters of each file name

    // Directory entries
    private int  fsizes[];      // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    
    /**
     * Instantiates this Directory to support a given number of files.
     * @param  maxInumber  The number of files this Directory should support.
     * @pre    The calling class has provided an appropriate maxInumber value.
     * @post   This Directory can support the specified number of files, which
     *          includes itself at index 0.
     */
    public Directory(int maxInumber) {  // directory constructor
        fsizes = new int[maxInumber];   // maxInumber = max files
        for (int i = 1; i < maxInumber; i++)
            fsizes[i] = 0;                  // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                  // entry (inode) 0 is "/"
        fsizes[0] = (short)root.length();   // fsize[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0);  // fnames[0] includes "/"
    } // end constructor

    
    /**
     * Initializes this Directory from a byte array that was previously created
     *  by directory2bytes(), assuming the same inode count.
     * @param  data  A byte array that describes a directory structure.
     * @pre    data supports the same number of files as this Directory.
     * @post   This Directory is a duplicate of the state of the Directory that
     *          created data.
     * @return The number of entries read from data.
     */
    public int bytes2directory(byte data[]) {
        int offset  = maxChars * 2 + 4,         // byte offset for each entry
            current,            // counter for the current entry to read
            entries = data.length / offset;     // number of entries to read
        // invalidate existing file entries
        for (int i = 0; i < fsizes.length; ++i) {
            fsizes[i] = 0;
        } // end for (; i < fsizes.length; )
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        for (int i = 0; i < entries; ++i) {
            current = SysLib.bytes2short(data, i * offset);
            fsizes[current] = SysLib.bytes2short(data, i * offset + 2);
            
            for (int j = 0; j < fsizes[current]; ++j) {
                fnames[current][j] =
                        (char)SysLib.bytes2short(data, i * offset + j * 2 + 4);
            } // end for (; j < fsizes[current]; )
        } // end for (; i < entries; )
        
        return entries;
    } // end bytes2directory(byte[])

    
    /**
     * Stores the contents of this directory in a byte array. Each file entry
     *  consumes 64 bytes in the array.
     * @pre    None.
     * @post   This Directory remains unchanged.
     * @return A byte array, suitable for storing to disk, that represents the
     *          file entries of this Directory.
     */
    public byte[] directory2bytes() {
        int offset  = maxChars * 2 + 4,     // entry offset marker
            current = 0,        // counter for the current entry to copy
            entries = 0;        // counter for number of entries to copy
        
        for (int i = 0; i < fsizes.length; ++i) {
            if (fsizes[i] > 0) {
                ++entries;
            } // end if (fsizes[i] > 0)
        } // end for (; i < fsizes.length; )
        // allocate space for file entries
        byte[] data = new byte[entries * offset];
        // converts Directory information into plain byte array and returns it
        // this byte array will be written back to disk
        // note: only meaningful directory information should be converted
        // into bytes.
        for (short i = 0; i < fsizes.length; ++i) {
            if (fsizes[i] > 0) {
                // entry found, copy it
                SysLib.short2bytes(i, data, current * offset);
                SysLib.short2bytes((short)fsizes[i],
                                   data, current * offset + 2);
                // copy filename by char
                for (short j = 0; j < fsizes[i]; ++j) {
                    SysLib.short2bytes((short)fnames[i][j],
                                    data, current * offset + j * 2 + 4);
                } // end for (; j < fsizes[i]; )
                
                ++current;
            } // end if (fsizes[i] > 0)
        } // end for (; i < fsizes.length; )
        
        return data;
    } // end directory2bytes()

    
    /**
     * Allocates the first unused inode number to a new file named by filename.
     * @param  filename  The name of the file to create.
     * @pre    filename does not name a file that exists in this Directory.
     * @post   This Directory contains a file entry for a file with the name
     *          specified by filename; an unused inode exists.
     * @return The inode number associated with the new file if it was created;
     *          -1 otherwise.
     */
    public short ialloc(String filename) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        if (namei(filename) == -1) {
            for (short i = 0; i < fsizes.length; ++i) {
                if (fsizes[i] == 0) {
                    fsizes[i] = filename.length();
                    filename.getChars(0, fsizes[i], fnames[i], 0);
                    return i;
                } // end if (fsizes[i] == 0)
            } // end for (; i < fsizes.length; )
        } // end if (namei(filename) != -1)
        // file name already in use or no inodes available
        return Kernel.ERROR;
    } // end ialloc(String)

    
    /**
     * Frees the inode of a file so that it may be deleted.
     * @param  iNumber  The inode number of the file to be deleted.
     * @pre    iNumber is within range of the total inodes.
     * @post   The specified inode is unregistered and its file entry lost.
     * @return true if the specified inode pointed to a file in this Directory;
     *          false otherwise.
     */
    public boolean ifree(short iNumber) {
        boolean found = false;
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if (iNumber > 0 && iNumber < fsizes.length) {
            found = fsizes[iNumber] > 0;
            fsizes[iNumber] = 0;
        } // end if (iNumber > 0...)
        
        return found;
    } // end ifree(short)

    
    /**
     * Provides the inode number of the file specified by filename.
     * @param  filename  The name of the file to locate.
     * @pre    filename specifies the name of a file that exists in this
     *          Directory.
     * @post   This Directory remains unchanged.
     * @return The inode number of the file specified by filename if such a
     *          file could be found; -1 otherwise.
     */
    public short namei(String filename) {
        String test;
        // returns the inumber corresponding to this filename
        for (short i = 0; i < fsizes.length; ++i) {
            if (fsizes[i] > 0) {
                test = new String(fnames[i], 0, fsizes[i]);
                
                if (filename.compareTo(test) == 0) {
                    return i;
                } // end if (filename.compareTo(test) == 0)
            } // end if (fsizes[i] > 0)
        } // end for (; i < fsizes.length; )
        // no entry found with name specified by filename
        return Kernel.ERROR;
    } // end namei(String)
} // end class Directory
