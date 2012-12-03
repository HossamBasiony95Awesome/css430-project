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
    private Directory directory;
//    private static FileTable filetable;
    
    
    /**
     * .
     * @param  diskBlocks  .
     * @pre    .
     * @post   .
     */
    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock( diskBlocks );
        directory  = new Directory( superblock.totalInodes );
//        filetable  = new FileTable( directory );
        int dirEnt  = open( "/", "r" );
        int dirSize = fsize( dirEnt );
        
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read( dirEnt, dirData );
            directory.bytes2directory( dirData );
        }
        
    close( dirEnt );
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
//        FileTableEntry ftEnt = filetable.falloc( filename, mode );
//        if ( mode == "w" )             // release all blocks belonging to this file
//        if ( deallocAllBlocks( ftEnt ) == false )
            return 0;
//        return ftEnt;
    } // end open(String, String)
    
    
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
