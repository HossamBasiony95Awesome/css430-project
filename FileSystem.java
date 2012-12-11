/*
 * @file    FileSystem.java
 * @brief   This class is the file system for ThreadOS. It maintains the
 *           superblock, inodes, and directory, all of which reside on disk,
 *           and a global file table, which only resides in memory. Free blocks
 *           are maintained on disk as a linked list of blocks that each point
 *           to the next free block in the list. The head of the list is held
 *           in the superblock.
 * @author  Brendan Sweeney, SID 1161836; Chris Grass
 * @date    December 14, 2012
 */
import java.util.Vector;


public class FileSystem {
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    public final static int DEFAULT_BLOCKS = 1000;
    public final static int DEFAULT_FILES  = 48;
    
    public  Vector<Inode> inodes;
    private SuperBlock    superblock;
    private Directory     directory;
    private FileTable     filetable;
    
    
    /**
     * .
     * @param  diskBlocks  .
     * @pre    .
     * @post   .
     */
    public FileSystem(int diskBlocks) {
        superblock  = new SuperBlock(diskBlocks);
        directory   = new Directory(superblock.totalBlocks);
        filetable   = new FileTable(directory);
        inodes      = new Vector<Inode>(superblock.totalBlocks);
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        } // end if (dirSize > 0)
        
        close(dirEnt);
    } // end constructor
    
    
    /**
     * .
     * @param  files  .
     * @pre    .
     * @post   .
     * @return .
     */
    public boolean format(int files) {
        int inodesPerBlock = Disk.blockSize / Inode.iNodeSize;
        
        // sanitize input
        if (files < 1) {
            files = DEFAULT_FILES;
        } // end if (files < 1)
        
        // ensure full use of last inode block
        if (files % inodesPerBlock != 0) {
            files += inodesPerBlock - files % inodesPerBlock;
        } // end if (files % inodesPerBlock != 0)
        
        // ensure file system not in use
        if (!filetable.fempty()) {
            SysLib.cerr("Error: cannot format, disk in use\n");
            return false;
        } // end if (!filetable.fempty())
        
        superblock.inodeBlocks = files;
        superblock.freeList    = files / inodesPerBlock + 1;
    	superblock.format(DEFAULT_BLOCKS);
        inodes    = new Vector<Inode>(files);
        directory = new Directory(files);
        
        return true; //SysLib.sync() == Kernel.OK;
    } // end format(int)
    
    
    /**
     * .
     * @param  fileName  .
     * @param  mode  .
     * @pre    .
     * @post   .
     * @return .
     * CGRASS NOTE - SHOULD WE HAVE Inode.flag that declares open/closed Inode?
     */
    public final FileTableEntry open(String filename, String mode) {
        FileTableEntry ftEnt = filetable.falloc( filename, mode );
        ftEnt.inode.count++;
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
     * @return false on error.
     */
    public final boolean close(FileTableEntry ftEnt) {
        if (ftEnt != null) {
            ftEnt.inode.count--;
    		return filetable.ffree(ftEnt);
        } // end if (ftEnt != null)
        
        return false;
    } // end close(FileTableEntry)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    public final int fsize(FileTableEntry ftEnt) {
        if (ftEnt != null) {
            return ftEnt.inode.length;
        } // end if (ftEnt != null)
        
        return Kernel.ERROR;
    } // end fsize(FileTableEntry)
    
    /**
     * .
     * @param  ftEnt  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return buffer.length.
     */
    public int read(FileTableEntry ftEnt, byte buffer[]) {
	if (ftEnt == null)
            return Kernel.ERROR;
    	int readLength = buffer.length; //total amount to read
    	int seekPtr = ftEnt.seekPtr;
    	if((ftEnt.inode.length-seekPtr)<readLength)
    		readLength = ftEnt.inode.length-seekPtr;
    	
    	byte[] reader = new byte[512];
    	if(readLength<=512){
    		int blockLoc = ftEnt.inode.findTargetBlock(seekPtr);
    		SysLib.rawread(blockLoc, reader); 
        	System.arraycopy(reader, seekPtr, buffer, 0, readLength); //copy to buffer
        	return readLength;
    	}    	
    	//if multiple blocks need to be read
    	else{							//continue reading from indirect array
    		int j = 0;
    		int bufferRead = 0;			//amount read so far
    		int nextReadLength = 0;		//amount to read this iteration
    		
    		//while length of buffer being read is less than inode length
    		//continue with next indirect address and add to buffer
    		while(bufferRead < readLength){
    			
    			//if near end of file, shorten readLength
    			if((ftEnt.inode.length-seekPtr)<readLength) 
    	    		nextReadLength = ftEnt.inode.length-seekPtr;    	    		
    			else
    				nextReadLength = 512;
    			int block = ftEnt.inode.findTargetBlock(bufferRead+seekPtr);
        		SysLib.rawread(block, reader);				//read from disk
        		//copy reader to end of buffer
        		System.arraycopy(reader, 0, buffer, bufferRead, nextReadLength); 
        		bufferRead += nextReadLength;
        		ftEnt.seekPtr += nextReadLength;
        		j++;
    		}
    		return readLength;
    	}    	
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
    	int fileLoc = ftEnt.seekPtr;
    	int buffSize = buffer.length;
    	int numOfBlocks = buffSize/512+1;
    	int nextBlockSize = 0;
    	int bufferWritten = 0;
    	String mode = ftEnt.mode;
    	if(mode.equals("w")||mode.equals("w+")){ 	//if writing to start of file
    		for(int i = 0; i<numOfBlocks && i<11; i++){		//for direct nodes    
    			
    			if(i+1==numOfBlocks)				//last block
    				nextBlockSize = buffSize%512;
    			else
    				nextBlockSize = 512;
    			
    			
    			byte[] writer = new byte[512];
    			short nextBlock;
    			
    			//if outside existing boundary, register new block
    			if(mode.equals("w")|| ftEnt.inode.length < (buffSize-bufferWritten)+fileLoc){
    				nextBlock = (short) superblock.getFreeBlock();
    				ftEnt.inode.registerTargetBlock(nextBlockSize, nextBlock);
    			}
    			else{ // inside current boundary so find block
    				nextBlock = (short) ftEnt.inode.findTargetBlock(fileLoc+bufferWritten);
    				SysLib.rawread(nextBlock, writer);
    			}
    			
    			//copy segment of buffer to writer
        		System.arraycopy(buffer, bufferWritten, writer, (fileLoc+512)%512, nextBlockSize);
        		//write buffer segment to nextFreeBlock on disk
        		SysLib.rawwrite(nextBlock, writer);  
        		ftEnt.seekPtr += nextBlockSize;
        		bufferWritten += nextBlockSize;
        		ftEnt.inode.length += nextBlockSize;
        		ftEnt.inode.toDisk(ftEnt.iNumber);
        		fileLoc +=nextBlockSize;
    		}
    		if(numOfBlocks<12){						//if only direct blocks used    			
    			return bufferWritten;
    		}
    		else{ //register index block and fill with rest of buffer
    			int k = 0;
    			if(ftEnt.inode.indirect==-1){
    				short indexBlock = (short) superblock.getFreeBlock();
    				ftEnt.inode.registerIndexBlock(indexBlock);
    			}
    			//while Inode.length is less than buffer.length, continue
    			//writing to disk    			
    			while(ftEnt.inode.length < buffer.length ){
    				int currentInodeLength = ftEnt.inode.length;
    				fileLoc = ftEnt.seekPtr;
    				
    				if (numOfBlocks-12 == k)			// last block add remainder to length
    					nextBlockSize = buffSize%512;
        			else								
        				nextBlockSize = 512;
    				
        			short nextBlock;    				
            		byte[] writer = new byte[512];
            		
            		//register new indirect block if outside current boundary
            		if(mode.equals("w")|| ftEnt.inode.length < (buffSize-bufferWritten)+fileLoc){
        				nextBlock = (short) superblock.getFreeBlock();
        				ftEnt.inode.registerTargetBlock(nextBlockSize, nextBlock);
        			}
        			
        			else{ // inside current boundary so find block
        				nextBlock = (short) ftEnt.inode.findTargetBlock(fileLoc+bufferWritten);
        				SysLib.rawread(nextBlock, writer);
        			}
            		
            		System.arraycopy(buffer,bufferWritten,writer,(bufferWritten+512)%512,nextBlockSize);
            		SysLib.rawwrite(nextBlock,writer);
            		
            		ftEnt.seekPtr += nextBlockSize;
            		bufferWritten += nextBlockSize;
            		ftEnt.inode.length += nextBlockSize;
            		ftEnt.inode.toDisk(ftEnt.iNumber);
            		k++;
        		}
    			return buffer.length;
    		}
    	}
//    	else if(ftEnt.mode.equals("w+")){
//    		return writePlus(ftEnt,buffer);
//    	}
    		return append( ftEnt, buffer);
    } // end write(int, byte[])
    
    
//    private int writePlus(FileTableEntry ftEnt, byte buffer[]){
//    	int fileLoc = ftEnt.seekPtr;
//    	int buffSize = buffer.length;
//    	int numOfBlocks = buffSize/512+1;
//    	int nextBlockSize = 0;
//    	
//    	for(int i = 0; i < 11; i++){
//    		if(i+1==numOfBlocks)				//last block
//				nextBlockSize = buffSize%512;
//			else
//				nextBlockSize = 512;
//    		
//    		
//    	}
//    	
//    	
//    	return 1;
//    }
    
    
    private int append(FileTableEntry ftEnt, byte buffer[]){
    	int fileLoc = ftEnt.seekPtr;
    	int buffSize = buffer.length;
    	int numOfBlocks = buffSize/512+1;
    	boolean blockOverlap = false;
    	int writeLength = fileLoc+buffSize;;
    	int lastInodeBlock = ftEnt.inode.findTargetBlock(buffSize+fileLoc);
    	if((fileLoc+buffSize)>512)
    		blockOverlap = true;
    	if(blockOverlap == false){		//simple case appending to single block
    		byte[] writer = new byte[512];
    		SysLib.rawread(lastInodeBlock, writer);
    		System.arraycopy(buffer,0,writer,fileLoc,buffSize);
    		SysLib.rawwrite(lastInodeBlock,writer);
    		ftEnt.inode.length +=buffSize;
    		ftEnt.inode.toDisk(ftEnt.iNumber);
    	}
    	
    	return writeLength;
    }
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  offset  .
     * @param  whence  .
     * @pre    .
     * @post   .
     * @return .
     * whence == SEEK_SET (0): if offset is positive and less than 
     * the size of the file, set the file's seek pointer to offset bytes from 
     * the beginning of the file and return success; otherwise return an error.
     * 
     * whence == SEEK_CUR (1): if offset is positive and less than or equal to 
     * the number of bytes between the current seek pointer and the end of the 
     * file, offset is added to the current seek pointer, and the new seek 
     * pointer position is the value returned; if offset is negative and its 
     * absolute value is less than or equal to the number of bytes between the 
     * current seek pointer and the beginning of the file, offset is subtracted 
     * from the current seek pointer, and the new seek pointer position is the 
     * value returned; otherwise return an error.
     * 
     * whence == SEEK_END (2): if offset is negative and less than the size of 
     * the file, set the file's seek pointer to offset bytes from the end of 
     * the file and return success; otherwise return an error.
     */
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        if (ftEnt == null)
            return Kernel.ERROR;
        
    	int currentPtr = ftEnt.seekPtr;
    	int fileLength = ftEnt.inode.length;
    	switch(whence){
    	case 0:
    		if(offset < fileLength && offset >= 0 ){
    			ftEnt.seekPtr=offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return Kernel.ERROR;
    	case 1:
    		if(offset > 0 && offset <= fileLength - currentPtr){
    			ftEnt.seekPtr = currentPtr + offset;
    			return ftEnt.seekPtr;
    		}
    		else if(offset<0 && (offset*-1) <= currentPtr){
    			ftEnt.seekPtr = currentPtr - offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return Kernel.ERROR;
    	case 2:
    		if(offset < 0 && offset < fileLength){
    			ftEnt.seekPtr = fileLength + offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return Kernel.ERROR;
    	default:
    		return Kernel.ERROR;
    	}
    } // end seek(FileTableEntry, int, int)
    
    
    /**
     * .
     * @param  fileName  .
     * @pre    .
     * @post   .
     * @return .
     */
    public boolean delete(String fileName) {
        return true;
    } // end delete(String)
    
    
    /**
     * .
     * @pre    .
     * @post   .
     * @return .
     */
    public boolean sync() {
        if (!filetable.fempty()) {
            SysLib.cerr("Error: disk in use...syncing anyway\n");
//            return false;
        } // end if (!filetable.fempty())
        
        superblock.sync();
        FileTableEntry dirEnt = open("/", "w");
        write(dirEnt, directory.directory2bytes());
        close(dirEnt);
        
        return true;
    } // end sync()
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        return ftEnt != null;
    } // end deallocAllBlocks(FileTableEntry)
} // end class FileSystem

