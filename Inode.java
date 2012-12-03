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
		int offset = ( iNumber % 16 ) * iNodeSize; 	
		//byte[] iNodeData = new byte[iNodeSize]; //allocate block for data
		byte[] originalData = new byte[Disk.blockSize]; 	
		
	    SysLib.rawread( blkNumber, originalData ); //read original block	    
		SysLib.int2bytes(length, originalData, offset); //write length
		offset +=4;
		SysLib.short2bytes(count, originalData, offset);	//write count
		offset +=2;
		SysLib.short2bytes(flag, originalData, offset);	//write flag
		offset +=2;
		for ( int i = 0; i < directSize; i++ ) { //write direct pointers
		      SysLib.short2bytes( direct[i],originalData, offset );
		      offset +=2;
		    }
		SysLib.short2bytes(indirect, originalData, offset);	//write indirect pointer
		SysLib.rawwrite(blkNumber, originalData);			//write data to disk
	}
}
