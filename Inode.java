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
	private short nextIndirectPointer;			   //count of indirect pointers

	Inode () {                        // a default constructor (new file)
		length = 0;					  // number of bytes in file
		count = 0;					  // number of threads pointing to file

		flag = 1;					  			//Flag Modes:
		for ( int i = 0; i < directSize; i++ )	//-1 = ready for deletion
			direct[i] = -1;						//0 = not in use
		indirect = -1;							//1 = in use
												//2 = thread currently reading
												//3 = thread currently writing		
	}
	
    /** 
     * Overloaded Constructor
     * @param  iNumber represents iNumber of inode being read from disk  .
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
	    if(length/512-11 > 0)
	    	nextIndirectPointer = (short) (length/512-11);
	    else
	    	nextIndirectPointer = 0;
	  }
	
    /** 
     * toDisk
     * @param iNumber represents iNumber to be written to disk  .
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
	
    /** 
     * findIndexBlock
     * @param  .
     * @pre    .
     * @post   .
     * @return indirect block number
     */
	int findIndexBlock(){
		return indirect;
	}
    /** 
     * registerIndexBlock
     * @param short indexBlockNumber   .
     * @pre    .
     * @post   .
     * sets indirect block number to param
     */
	boolean registerIndexBlock( short indexBlockNumber ){
		indirect = indexBlockNumber;
		nextIndirectPointer = 0;
		return true;
	}
    /** 
     * findTargetBlock
     * @param int offset   .
     * @pre    .
     * @post   .
     * @return int pointer to targetBlock
     * in this context, offset will be byte index location in file. 
     * TargetBlock = offset/512. If TargetBlock > 11, we must look in indirect
     * block for byte info.
     */
	int findTargetBlock( int offset ){
		int targetBlock = offset/512;
		if (targetBlock<11){
			if (direct[targetBlock] < 0)
				return -1;
			else					
				return direct[targetBlock];
		}
		else									//look in indirect
			if(indirect == -1)
				return -1;
			else
				return scanIndirect(offset);
	}
    /** 
     * registerTargetBlock
     * @param int numBytes, short targetBlockNumber   .
     * @pre    .
     * @post   .
     * @return returns 0 on success, -1 on failure
     *
     */
	int registerTargetBlock( int numBytes, short targetBlockNumber){
		//if beyond maxFileSize or beyond totalBlocks
		if (targetBlockNumber > 1000 || numBytes > 512*11 + 512 * 256)
			return -1;
		for (int i =0; i<11;i++){
			if (direct[i] < 0){
				direct[i]=targetBlockNumber;
				return 0;
			}			
		}
		//if beyond direct blocks, write to indirect block
		writeIndirect(targetBlockNumber);
		return 0;
	}
    /** 
     * unregisterIndexBlock
     * @param    .
     * @pre    .
     * @post   .
     * @return byte array of block info stored in indirect.
     */
	byte[] unregisterIndexBlock(){
		byte[] indirectArray = new byte[512];
		SysLib.rawread(indirect,indirectArray);
		for (int i = 0; i < 512; i++){
			indirectArray[i] = 0;
		}
		nextIndirectPointer = 0;
		return indirectArray;
	}
	
    /** 
     * scanIndirect
     * @param  int offset  .
     * @pre    .
     * @post   .
     * @return Block information related to direct pointer 
     *			inside indirect pointer
     * Finds block information by scanning through index block
     * for appropriate offset. Offset/blockSize*shortSize = location. Read in 
     * that location data and return int representing blockNum.
     */
	private int scanIndirect(int offset){
		int targetBlock = -1;
		byte directLoc = (byte) ((offset/512-11)*2);	//determine loc in indirectArray
		
		//if out of current range
		if(offset>=length)
			return -1;
		
		byte[] indirectArray = new byte[512];	
		SysLib.rawread(indirect, indirectArray); //read inderectArray
		targetBlock = indirectArray[directLoc+1]; //find directBlock data
		return targetBlock; 
	}
    /** 
     * writeIndirect
     * @param  short targetBlockNum  .
     * @pre    .
     * @post   .
     * Writes block information by writing to next free block. Increment
     * nextIndirectPointer variable. 
     */
	private void writeIndirect(short targetBlockNum){	
		byte[] indirectArray = new byte[512];	
		SysLib.rawread(indirect, indirectArray); //read inderectArray
		SysLib.short2bytes(targetBlockNum, indirectArray, (int) nextIndirectPointer*2);
		SysLib.rawwrite(indirect, indirectArray);	
		nextIndirectPointer++;
	}
}

