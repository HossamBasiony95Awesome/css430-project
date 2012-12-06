/*
 * @file    Directory.java
 * @brief   This class is the directory structure for a simple filesystem.
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 12, 2012
 */
public class Directory {
    private static int maxChars = 30;   // max characters of each file name

    // Directory entries
    private int  fsizes[];      // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    
    /**
     * .
     * @param  maxInumber  .
     * @pre    .
     * @post   .
     */
    public Directory(int maxInumber) {  // directory constructor
        fsizes = new int[maxInumber];   // maxInumber = max files
        for (int i = 1; i < maxInumber; i++)
            fsizes[i] = 0;                  // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                  // entry (inode) 0 is "/"
        fsizes[0] = root.length();          // fsize[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0);  // fnames[0] includes "/"
    } // end constructor

    
    /**
     * .
     * @param  data  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int bytes2directory(byte data[]) {
        int offset  = maxChars * 2 + 4,         // i offset for entries
            current,            // counter for the current entry to read
            entries = data.length / offset;     // number of entries to read
        // invalidate existing file entries
        for (int i = 0; i < fsizes.length; ++i) {
            fsizes[i] = 0;
        } // end for (; i < fsizes.length; )
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        for (int i = 0; i < entries; ++i) {
            current = SysLib.bytes2int(data, i * offset);
            fsizes[current] = SysLib.bytes2int(data, i * offset + 2);
            
            for (int j = 0; j < fsizes[current]; ++j) {
                fnames[current][j] =
                        (char)SysLib.bytes2short(data, i * offset + j * 2 + 4);
            } // end for (; j < fsizes[current]; )
        } // end for (; i < entries; )
        
        return entries;
    } // end bytes2directory(byte[])

    
    /**
     * .
     * @pre    .
     * @post   .
     * @return .
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
     * .
     * @param  filename  .
     * @pre    .
     * @post   .
     * @return .
     */
    public short ialloc(String filename) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        for (short i = 0; i < fsizes.length; ++i) {
            if (fsizes[i] == 0) {
                fsizes[i] = filename.length();
                filename.getChars(0, fsizes[i], fnames[i], 0);
                return i;
            } // end if (fsizes[i] == 0)
        } // end for (; i < fsizes.length; )
        
        return -1;
    } // end ialloc(String)

    
    /**
     * .
     * @param  iNumber  .
     * @pre    .
     * @post   .
     * @return .
     */
    public boolean ifree(short iNumber) {
        boolean found = fsizes[iNumber] > 0;
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        fsizes[iNumber] = 0;
        
        return found;
    } // end ifree(short)

    
    /**
     * .
     * @param  filename  .
     * @pre    .
     * @post   .
     * @return .
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
        
        return -1;
    } // end namei(String)
} // end class Directory
