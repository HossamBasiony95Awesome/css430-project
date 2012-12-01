/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author brendan
 */
public class Superblock {
    public int totalBlocks;     // the number of disk blocks
    public int totalInodes;     // the number of inodes
    public int freeList;        // the block number of the free list's head
    
    
    public Superblock(int blocks, int inodes, int free) {
        totalBlocks = blocks;
        totalInodes = inodes;
        freeList    = free;
    } // end constructor
    
    
    public int toDisk() {
        byte[] buffer = new byte[Disk.blockSize];
        
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList,    buffer, 8);
        
        return SysLib.rawwrite(0, buffer);
    } // end toDisk()
} // end class Superblock
