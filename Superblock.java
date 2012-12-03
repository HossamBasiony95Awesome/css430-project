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
} // end class Superblock
