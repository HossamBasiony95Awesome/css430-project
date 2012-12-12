/*
 * @file    SuperBlock.java
 * @brief   This class is a superblock for a virtual hard disk. It only needs
 *           to maintain three variables: total blocks that make up the disk,
 *           total inodes in the file system (maximum number of files), and the
 *           first unused disk block. Despite the small size, the superblock
 *           requires an entire block (block 0) when stored to disk.
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 14, 2012
 */
public class SuperBlock {
    private final int defaultTotalInodes = 64;
    public int totalBlocks;     // the number of disk blocks
    public int totalInodes;     // the number of inodes
    public int freeList;        // the block number of the free list's head
    
    
    /**
     * Instantiates a SuperBlock with a given number of disk blocks. If an
     *  existing superblock on disk exists with the specified block count, then
     *  that superblock shall provide the remaining values for this one.
     *  Otherwise, the disk is formatted to allot the proper blocks and the
     *  remaining member variables are temporarily set to default values.
     * @param  diskBlocks  The number of data blocks on the containing disk.
     * @pre    diskBlocks is a positive number; if a format is needed, then the
     *          calling class will set totalInodes and freeList to appropriate
     *          values when it calls SuperBlock.format().
     * @post   Block 0 of the virtual disk contains a superblock which is
     *          represented by this one.
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
            totalInodes = defaultTotalInodes;
            freeList    = totalInodes / (Disk.blockSize / Inode.iNodeSize) + 1;
            format(diskBlocks);
        } // end if (totalBlocks != diskBlocks...)
    } // end constructor
    
    
    /**
     * Writes this SuperBlock to block 0 of the virtual disk.
     * @pre    None.
     * @post   Block 0 of the virtual disk contains a superblock which is
     *          represented by this one.
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
     * Formats a virtual disk to contain only a superblock and a free list. The
     *  free list is a linked list of all unused disk blocks, where each block
     *  in the list contains, in its first few bytes, the number of the next
     *  block in the list. The number of the first block in this list is held
     *  by freeList.
     * @pre    numBlocks is a positive number; the calling class has set
     *          totalInodes and freeList to appropriate values.
     * @post   The virtual disk contains only a superblock in block 0, which is
     *          represented by this one, and a sequence of unused blocks which
     *          each contain the number of the following block.
     */
    public void format() {
        format(FileSystem.DEFAULT_BLOCKS);
    } // end format()
    
    
    /**
     * Formats a virtual disk to contain only a superblock and a free list. The
     *  free list is a linked list of all unused disk blocks, where each block
     *  in the list contains, in its first few bytes, the number of the next
     *  block in the list. The number of the first block in this list is held
     *  by freeList.
     * @param  numBlocks  The number of data blocks on the containing disk.
     * @pre    numBlocks is a positive number; the calling class has set
     *          totalInodes and freeList to appropriate values.
     * @post   The virtual disk contains only a superblock in block 0, which is
     *          represented by this one, and a sequence of unused blocks which
     *          each contain the number of the following block.
     */
    public void format(int numBlocks) {
        totalBlocks   = numBlocks;
        byte[] buffer = new byte[Disk.blockSize];
        
        // initialize free list and write it to disk
        for (int i = freeList; i < totalBlocks - 1; ++i) {
            SysLib.int2bytes(i + 1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        } // end for (; i < totalBlocks - 1; )
        
        SysLib.int2bytes(-1, buffer, 0);
        SysLib.rawwrite(totalBlocks - 1, buffer);
        sync();     // write superblock to disk
    } // end format(int)
    

    /**
     * Provides the number of the first unused block on disk and removes that
     *  block from the head of the free list.
     * @pre    The free list contains an unused block.
     * @post   freeList points to the block following the previous head of the
     *          list, or contains -1 if the list is now empty.
     * @return The number of the first unused block if one is available; -1
     *          otherwise.
     */
    public int getFreeBlock() {
    /* dequeue top block 
        in freelist */
        byte[] buffer = new byte[Disk.blockSize];
        int    temp   = freeList;
        
        if (freeList != -1) {
            SysLib.rawread(freeList, buffer);
            freeList = SysLib.bytes2int(buffer, 0);
        } // end if (freeList != -1)
        
        return temp;
    } // end getFreeBlock()
    

    /**
     * Returns a block to the free list at the head.
     * @param  oldBlockNumber  Number of the block to be placed at the head of
     *                          the free list.
     * @pre    oldBlockNumber is not already in the free list.
     * @post   oldBlockNumber sits at the head of the free list and points to
     *          the previous head.
     * @return true of the specified block could be written to the free list;
     *          false otherwise.
     */
    public boolean returnBlock(int oldBlockNumber) {
    /* enqueue oldBlockNumber 
        to top of freelist */
        if (oldBlockNumber < 1 || oldBlockNumber > totalBlocks) {
            return false;
        } // end if (oldBlockNumber < 1...)
        
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.int2bytes(freeList, buffer, 0);
        freeList = oldBlockNumber;
        
        return SysLib.rawwrite(oldBlockNumber, buffer) == Kernel.OK;
    } // end returnBlock(int)
} // end class SuperBlock
