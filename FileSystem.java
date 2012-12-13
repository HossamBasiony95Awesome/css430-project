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
        directory   = new Directory(superblock.totalInodes);
        filetable   = new FileTable(directory);
        inodes      = new Vector<Inode>(superblock.totalInodes);
        
        if (superblock.freeList ==
                superblock.totalInodes / (Disk.blockSize / Inode.iNodeSize) + 1) {
            byte[] buffer = new byte[Disk.blockSize];
            Inode dir     = new Inode();
            dir.length    = 64;
            dir.direct[0] = (short)superblock.freeList;
            dir.toDisk((short)0);
            System.arraycopy(directory.directory2bytes(), 0, buffer, 0, 64);
            SysLib.rawwrite(superblock.freeList++, buffer);
        } // end if (superblock.freeList ==...)
        
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
        byte[] buffer      = new byte[Disk.blockSize];
        
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
        
        superblock.totalInodes = files;
        superblock.freeList    = files / inodesPerBlock + 1;
    	superblock.format(DEFAULT_BLOCKS);
        inodes    = new Vector<Inode>(files);
        directory = new Directory(files);
        Inode dir = new Inode();
        dir.length    = 64;
        dir.direct[0] = (short)superblock.freeList;
        dir.toDisk((short)0);
        System.arraycopy(directory.directory2bytes(), 0, buffer, 0, 64);
        SysLib.rawwrite(superblock.freeList++, buffer);
        
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
//        ftEnt.inode.count++;
        if ( mode.compareTo("w")==0)             // release all blocks belonging to this file
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
//            ftEnt.inode.flag = 0;
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
    	int origSeekPtr = ftEnt.seekPtr;
    	if((ftEnt.inode.length-origSeekPtr)<readLength)
    		readLength = ftEnt.inode.length-origSeekPtr;
    	
    	byte[] reader = new byte[512];
    	if(readLength<=512){
    		int blockLoc = ftEnt.inode.findTargetBlock(origSeekPtr);
    		SysLib.rawread(blockLoc, reader); 
        	System.arraycopy(reader, origSeekPtr%512, buffer, 0, readLength); //copy to buffer
        	return readLength;
    	}    	
    	//if multiple blocks need to be read
    	else{							//continue reading from indirect array
    		int bufferRead = 0;			//amount read so far
    		int nextReadLength = 0;		//amount to read this iteration
    		
    		//while length of buffer being read is less than inode length
    		//continue with next indirect address and add to buffer
    		while(bufferRead < readLength){
    			int bytesLeft = readLength-ftEnt.seekPtr;
    			//if near end of file, shorten readLength
    			if((bytesLeft)<(512)) 
    	    		nextReadLength = bytesLeft;    	    		
    			else
    				nextReadLength = 512;
    			int block = ftEnt.inode.findTargetBlock(bufferRead+origSeekPtr);
        		SysLib.rawread(block, reader);				//read from disk
        		//copy reader to end of buffer
        		System.arraycopy(reader, 0, buffer, bufferRead, nextReadLength); 
        		bufferRead += nextReadLength;
        		ftEnt.seekPtr += nextReadLength;
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
    	int origSeekPtr = fileLoc;
    	int buffSize = buffer.length;
    	int numOfBlocks;
    	if (fileLoc+512 % 512 == 0)
    		//numofBlocks = ((origSeekPtr+buffSize-1)/512)+1;
    		numOfBlocks = ((buffSize-1)/512)+1;
    	else
    		numOfBlocks = ((buffSize + (origSeekPtr % 512)-1)/512)+1;
    	int nextBlockSize = 0;
    	int bufferWritten = 0;    	
    	String mode = ftEnt.mode;
    	//if(mode.equals("w")||mode.equals("w+")){ 	//if writing to start of file
    		for(int i = 0; i<numOfBlocks && fileLoc < 11*512; i++){		//for direct nodes    
    			
    			//last block or piece of first block in buffer
    			if(i+1==numOfBlocks)			
    				nextBlockSize = buffSize%512;
    			else if((fileLoc+512) % 512 != 0){
    				//if first block of buffer and buffer > 2 blocks
    				if(buffSize+origSeekPtr > 512) 
    					nextBlockSize = 512-origSeekPtr;
    				else
    					nextBlockSize = buffSize;
    				}
    			else
    				nextBlockSize = 512;
    			
    			byte[] writer = new byte[512];
    			short nextBlock = (short) ftEnt.inode.findTargetBlock(origSeekPtr+bufferWritten);
    			
    			//if outside existing boundary, register new block
    			if(mode.equals("w")|| nextBlock==-1){
    				nextBlock = (short) superblock.getFreeBlock();
    				ftEnt.inode.registerTargetBlock(nextBlockSize, nextBlock);
    			}
    			else{ // inside current boundary so find block
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
    		if(bufferWritten==buffSize){			//if only direct blocks used    			
    			return bufferWritten;
    		}
    		else{ //register index block and fill with rest of buffer
    			int k = 0;
    			int typeFlag =0;
//    			//adjust numOfBlocks for mode "a"
//    			if(mode.equals("a"))
//					numOfBlocks = (buffer.length-1)/512+1;
    			
    			if(ftEnt.inode.indirect==-1){
    				short indexBlock = (short) superblock.getFreeBlock();
    				ftEnt.inode.registerIndexBlock(indexBlock);
    			}
    			
    			//while buffer.length is less than bufferWritten,
    			//continue writing to disk       
    			// removed - ftEnt.inode.length < buffer.length || (mode.equals("a") 
    			while(bufferWritten<buffer.length){
    				fileLoc = ftEnt.seekPtr;    				
    				
    				// if last block or first small block
    				if (numOfBlocks-12 <= k){
    					//data after direct nodes
    					if(numOfBlocks>11){
    						nextBlockSize = buffSize-bufferWritten;
    						typeFlag = 0;	
    					}
    					//first node of data spanning two blocks
    					else if((fileLoc) % 512 + buffSize > 512){
    						nextBlockSize = 512-(fileLoc+512) % 512;
    						typeFlag = 1;
    					}
    					else{//second node of data spanning two blocks
    						nextBlockSize = buffSize-bufferWritten;
    						typeFlag = 2;
    					}
    				}
        			else								
        				nextBlockSize = 512;
    				
    				short nextBlock = (short) ftEnt.inode.findTargetBlock(origSeekPtr+bufferWritten);				
            		byte[] writer = new byte[512];
            		
            		//register new indirect block if outside current boundary
            		if(mode.equals("w")|| nextBlock==-1){
        				nextBlock = (short) superblock.getFreeBlock();
        				ftEnt.inode.registerTargetBlock(nextBlockSize, nextBlock);
        			}
        			
        			else{ // inside current boundary so find block
        				SysLib.rawread(nextBlock, writer);
        			}
            		if(typeFlag == 0) //normal condition or last block piece
            			System.arraycopy(buffer,bufferWritten,writer,(bufferWritten)%512,nextBlockSize);
            		else if(typeFlag ==1)			//first block in condition where data starts mid block and overlaps.
            			System.arraycopy(buffer,bufferWritten,writer,origSeekPtr%512,nextBlockSize);
            		else
            			System.arraycopy(buffer,bufferWritten,writer,0,nextBlockSize);
            		SysLib.rawwrite(nextBlock,writer);
            		
            		ftEnt.seekPtr += nextBlockSize;
            		bufferWritten += nextBlockSize;
            		ftEnt.inode.length += nextBlockSize;
            		ftEnt.inode.toDisk(ftEnt.iNumber);
            		k++;
        		}
    			return bufferWritten;
    		
    	}
    } // end write(int, byte[])
    
    
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
        FileTableEntry ftEnt = open(fileName, "r");
        close(ftEnt);
        ftEnt.inode.flag = -1;
        while(ftEnt.inode.count > 0)
            ;

        directory.ifree(ftEnt.iNumber);        
        FileTableEntry ent = filetable.findFtEnt(ftEnt.iNumber);
        deallocAllBlocks(ent);
        if(ent.inode != null)
            ent.inode.flag = -1;
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
        if(ftEnt == null)
            return false;
        for(int i = 0; i<11; i++){
            if(ftEnt.inode.direct[i]==-1)
                return true;
            if(!superblock.returnBlock(i))
                return false;
        }
        byte[] freeBlocks = ftEnt.inode.unregisterIndexBlock();

        for (int i = 0; i<ftEnt.inode.length/512-11; i++){
            int blockNum = (int) freeBlocks[i*2];
            if(!superblock.returnBlock(blockNum))
                return false;
        }
        return true;
    } // end deallocAllBlocks(FileTableEntry)
} // end class FileSystem

