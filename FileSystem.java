/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author brendan
 */
import java.util.Vector;


public class FileSystem {
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    public final static int DEFAULT_BLOCKS = 1000;
    public final static int DEFAULT_FILES  = 48;
    
    public static Vector<Inode> inodes;
//    public static FileTable table;
    private Superblock blockZero;
    
    
    /**
     * .
     * @param  data  .
     * @pre    .
     * @post   .
     */
    public FileSystem(int thing) {
        initSuperblock();
        initInodes();
    } // end constructor
    
    
    /**
     * .
     * @param  files  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int format(int files) {
//        if (table.fempty()) {
        return 0;
    } // end format(int)
    
    
    /**
     * .
     * @param  fileName  .
     * @param  mode  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int open(String fileName, String mode) {
        return -1;
    } // end open(String, String)
    
    
    /**
     * .
     * @param  fd  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int read(int fd, byte buffer[]) {
        return buffer.length;
    } // end read(int, byte[])
    
    
    /**
     * .
     * @param  fd  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int write(int fd, byte buffer[]) {
        return buffer.length;
    } // end write(int, byte[])
    
    
    /**
     * .
     * @param  fd  .
     * @param  offset  .
     * @param  whence  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int seek(int fd, int offset, int whence) {
        return 0;
    } // end seek(int, int, int)
    
    
    /**
     * .
     * @param  fd  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int close(int fd) {
        return 0;
    } // end close(int)
    
    
    /**
     * .
     * @param  fileName  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int delete(String fileName) {
        return 0;
    } // end delete(String)
    
    
    /**
     * .
     * @param  fd  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int fsize(int fd) {
        return 0;
    } // end fsize(int)
    
    
    /**
     * .
     * @param  data  .
     * @pre    .
     * @post   .
     * @return .
     */
    private void initSuperblock() {
        int free  = DEFAULT_FILES * (Disk.blockSize / Inode.iNodeSize) + 2;
        blockZero = new Superblock();
    } // end initSuperblock()
    
    
    /**
     * .
     * @param  data  .
     * @pre    .
     * @post   .
     * @return .
     */
    private void initInodes() {
        Inode current;
        inodes = new Vector<Inode>();
        
        for (short i = 0; i < blockZero.totalInodes; ++i) {
            current = new Inode(i);
            inodes.add(current);
        } // end for (; i < blockZero.totalInodes; )
    } // end initInodes()
} // end class FileSystem
