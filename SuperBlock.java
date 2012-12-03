/*
 * @file    SuperBlock.java
 * @brief   .
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 12, 2012
 */
public class SuperBlock {
    private final int defaultTotalInodes = 64;
    public int totalBlocks;     // the number of disk blocks
    public int totalInodes;     // the number of inodes
    public int freeList;        // the block number of the free list's head
    
    
    public SuperBlock(int diskBlocks) {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread( 0, superBlock );
        totalBlocks = SysLib.bytes2int( superBlock, 0 );
        totalInodes = SysLib.bytes2int( superBlock, 4 );
        freeList    = SysLib.bytes2int( superBlock, 8 );
        if ( totalBlocks != diskBlocks 
                || totalInodes < 1 
                || freeList < 2 ) {
            totalBlocks = diskBlocks;
            SysLib.cerr( "Formatting\n" );
            SysLib.format(defaultTotalInodes);
        }
    } // end constructor
    
    
    public void sync() {
    /* write totalBlocks, inodeBlocks, freelist
        to disk */
    } // end sync()
    

    public void format() {
        
    } // end format()
    
    
    public void format( int numBlocks ) {
        
    } // end format(int)
    

    public int getFreeBlock() {
    /* dequeue top block 
        in freelist */
        return freeList;
    } // end getFreeBlock()
    

    public boolean returnBlock( int oldBlockNumber ) {
    /* enqueue oldBlockNumber 
        to top of freelist */
        return freeList > 0;
    } // end returnBlock(int)
} // end class SuperBlock
