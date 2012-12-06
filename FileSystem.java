/*
 * @file    FileSystem.java
 * @brief   .
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 12, 2012
 */
import java.util.Vector;


public class FileSystem {
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    public final static int DEFAULT_BLOCKS = 1000;
    public final static int DEFAULT_FILES  = 48;
    
    public static Vector<Inode> inodes;
    private SuperBlock superblock;
    private Directory  directory;
    private FileTable  filetable;
    
    
    /**
     * .
     * @param  diskBlocks  .
     * @pre    .
     * @post   .
     */
    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        directory  = new Directory(superblock.totalInodes);
        filetable  = new FileTable(directory);
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        
    close(dirEnt);
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
    public FileTableEntry open(String filename, String mode) {
        FileTableEntry ftEnt = filetable.falloc( filename, mode );
        if ( mode == "w" )             // release all blocks belonging to this file
        if ( deallocAllBlocks( ftEnt ) == false )
            return null;
        return ftEnt;
    } // end open(String, String)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int close(FileTableEntry ftEnt) {
        return 0;
    } // end close(int)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int fsize(FileTableEntry ftEnt) {
        return 0;
    } // end fsize(int)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int read(FileTableEntry ftEnt, byte buffer[]) {
        return buffer.length;
    } // end read(int, byte[])
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int write(FileTableEntry ftEnt, byte buffer[]) {
        return buffer.length;
    } // end write(int, byte[])
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  offset  .
     * @param  whence  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        return 0;
    } // end seek(int, int, int)
    
    
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
     * @param  fileName  .
     * @pre    .
     * @post   .
     * @return .
     */
    public void sync() {
    } // end sync()
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        return false;
    } // end deallocAllBlocks(FileTableEntry)
//    
//    
//    /**
//     * .
//     * @param  data  .
//     * @pre    .
//     * @post   .
//     * @return .
//     */
//    private void initSuperblock() {
//        int free  = DEFAULT_FILES * (Disk.blockSize / Inode.iNodeSize) + 2;
//        superblock = new SuperBlock();
//    } // end initSuperblock()
//    
//    
//    /**
//     * .
//     * @param  data  .
//     * @pre    .
//     * @post   .
//     * @return .
//     */
//    private void initInodes() {
//        Inode current;
//        inodes = new Vector<Inode>();
//        
//        for (short i = 0; i < superblock.totalInodes; ++i) {
//            current = new Inode(i);
//            inodes.add(current);
//        } // end for (; i < blockZero.totalInodes; )
//    } // end initInodes()
} // end class FileSystem
