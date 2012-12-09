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
    
    
    /**
     * .
     * @param  files  .
     * @pre    .
     * @post   .
     * @return .
     */
    public SuperBlock(int diskBlocks) {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList    = SysLib.bytes2int(superBlock, 8);
        if (totalBlocks != diskBlocks
                || totalInodes < 1
                || freeList < 2) {
            totalBlocks = diskBlocks;
            SysLib.cerr("Formatting\n");
            SysLib.format(defaultTotalInodes);
        } // end if (totalBlocks != diskBlocks...)
    } // end constructor
    
    
    /**
     * .
     * @pre    .
     * @post   .
     */
    public void sync() {
    /* write totalBlocks, inodeBlocks, freelist
        to disk */
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList,    buffer, 8);
        SysLib.rawwrite(0, buffer);
    } // end sync()
    

    /**
     * .
     * @pre    .
     * @post   .
     */
    public void format() {
        format(FileSystem.DEFAULT_BLOCKS);
    } // end format()
    
    
    /**
     * .
     * @param  numBlocks  .
     * @pre    .
     * @post   .
     */
    public void format(int numBlocks) {
        totalBlocks = numBlocks;
        freeList = 1;
        byte[] buffer = new byte[Disk.blockSize];
        for (int i = freeList; i < totalBlocks - 1; ++i) {
            SysLib.int2bytes(i + 1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        }
        SysLib.int2bytes(-1, buffer, 0);
        SysLib.rawwrite(totalBlocks - 1, buffer);
    } // end format(int)
    

    /**
     * .
     * @pre    .
     * @post   .
     * @return .
     */
    public int getFreeBlock() {
    /* dequeue top block 
        in freelist */
        byte[] buffer = new byte[Disk.blockSize];
        int temp = freeList;
        SysLib.rawread(freeList, buffer);
        freeList = SysLib.bytes2int(buffer, 0);
        return temp;
    } // end getFreeBlock()
    

    /**
     * .
     * @param  oldBlockNumber  .
     * @pre    .
     * @post   .
     * @return .
     */
    public boolean returnBlock(int oldBlockNumber) {
    /* enqueue oldBlockNumber 
        to top of freelist */
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.int2bytes(freeList, buffer, 0);
        freeList = oldBlockNumber;
        
        return SysLib.rawwrite(oldBlockNumber, buffer) == 0;
    } // end returnBlock(int)
} // end class SuperBlock
