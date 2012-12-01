/*
 * @file    Inode.java
 * @brief   This class is the Inode structure for a simple filesystem.
 * @author  Chris Grass
 * @date    December 12, 2012
 */

public class Inode {

	public final static int iNodeSize = 32;      // fixed to 32 bytes
	public final static int directSize = 11;     // # direct pointers

	public int length;                 // file size in bytes
	public short count;                // # file-table entries pointing to this
	public short flag;                 // how is this file (inode) being used?
	public short direct[] = new short[directSize]; // direct pointers
	public short indirect;                         // an indirect pointer

	Inode () {                        // a default constructor (new file)
		length = 0;
		count = 0;
		flag = 1;
		for ( int i = 0; i < directSize; i++ )
			direct[i] = -1;
		    indirect = -1;
	}
	
    /** 
     * Overloaded Constructor
     * @param  iNumber  .
     * @pre    .
     * @post   .
     * retrieves inode(iNumber) from disk and loads to memory. saves Inode data
     */
	Inode ( short iNumber ) {
	    int blkNumber = iNumber / 16 + 1; 	//determines block# on disk
	    byte[] data = new byte[Disk.blockSize]; 	
	    SysLib.rawread( blkNumber, data ); //read block corresponding to iNumber
	    int offset = ( iNumber % 16 ) * iNodeSize; 	
	    length = SysLib.bytes2int( data, offset );
	    offset += 4;
	    count = SysLib.bytes2short( data, offset );
	    offset += 2;
	    flag = SysLib.bytes2short( data, offset );
	    offset += 2;
	    for ( int i = 0; i < directSize; i++ ) {
	      direct[i] = SysLib.bytes2short( data, offset );
	      offset += 2;
	    }
	    indirect = SysLib.bytes2short( data, offset );
	  }
	
    /** 
     * toDisk
     * @param iNumber   .
     * @pre    .
     * @post   .
     * writes an iNode to disk
     */
	public void toDisk(short iNumber){
		int blkNumber = iNumber / 16 + 1; 	//determines block# on disk
		byte[] iNodeData = new byte[iNodeSize]; //allocate block for data
		SysLib.int2bytes(length, iNodeData, 0); //write length
		SysLib.int2bytes(count, iNodeData, 4);	//write count
		SysLib.int2bytes(flag, iNodeData, 6);	//write flag
		for ( int i = 0; i < directSize; i++ ) { //write direct pointers
		      SysLib.short2bytes( direct[i],iNodeData, 8+2*i );
		    }
		SysLib.short2bytes(indirect, iNodeData, 30);	//write indirect pointer
		SysLib.rawwrite(blkNumber, iNodeData);			//write data to disk
	}
}
