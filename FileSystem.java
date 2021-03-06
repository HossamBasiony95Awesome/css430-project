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
public class FileSystem {
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    public final static int DEFAULT_BLOCKS = 1000;
    public final static int DEFAULT_FILES  = 48;
    
    private SuperBlock superblock;
    private Directory  directory;
    private FileTable  filetable;
    
    
    /**
     * Initializes a FileSystem and ensures its data is commited to persistent
     *  storage.
     * @param  diskBlocks  Number of blocks supported by the disk on which this
     *                      file system will reside.
     * @pre    diskBlocks is within the bounds of the supporting disk.
     * @post   This FileSystem describes a simple file system whose superblock
     *          and directory are written to disk.
     */
    public FileSystem(int diskBlocks) {
        superblock  = new SuperBlock(diskBlocks);
        directory   = new Directory(superblock.inodeBlocks);
        filetable   = new FileTable(directory);
        
        // ensure directory has been written to disk
        if (superblock.freeList ==
                superblock.inodeBlocks /
                (Disk.blockSize / Inode.iNodeSize) + 1) {
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
     * Formats this FileSystem to support a specified number of files. Only the
     *  beginning of each data block on disk is overwritten. Inodes are not
     *  overwritten at all, but old inodes on disk may now have invalid
     *  references.
     * @param  files  The number of files to support via inodes.
     * @pre    files is significantly less than the number of blocks on disk.
     * @post   A new superblock and directory have been written to disk, able
     *          to support the specified number of files.
     * @return true if there are no open files and the format proceeds; false
     *          otherwise.
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
        
        superblock.inodeBlocks = files;
        superblock.freeList    = files / inodesPerBlock + 1;
    	superblock.format(DEFAULT_BLOCKS);
        directory = new Directory(files);
        Inode dir = new Inode();
        dir.length    = 64;
        dir.direct[0] = (short)superblock.freeList;
        dir.toDisk((short)0);
        System.arraycopy(directory.directory2bytes(), 0, buffer, 0, 64);
        SysLib.rawwrite(superblock.freeList++, buffer);
        
        return true;
    } // end format(int)
    
    
    /**
     * open().
     * @param  fileName representing file being opened.
     * @param  mode = r/w/w+/a.
     * @return ftEnt referencing fileTableEntry associated with fileName/mode.
     */
    public final FileTableEntry open(String filename, String mode) {
    	//allocate FileTableEntry with appropriate params. returns null on error
        FileTableEntry ftEnt = filetable.falloc( filename, mode );
        //if unable to allocate fileTableEntry, return null
        if(ftEnt==null)
        	return null;
        
     // if mode==w, release all blocks belonging to this file
        if ( mode.compareTo("w")==0)
        	if ( deallocAllBlocks( ftEnt ) == false )
                return null;
        return ftEnt;
    } // end open(String, String)
    
    
    /**
     * close().
     * @param  ftEnt
     * @return false on error, true on success.
     */
    public final boolean close(FileTableEntry ftEnt) {
        if (ftEnt == null) {
            return false;
        } // end if (ftEnt == null)
        //attempt to release ftEnt from filetable.
        return filetable.ffree(ftEnt);
    } // end close(FileTableEntry)
    
    
    /**
     * Returns the size of the file referenced by ftEnt, if it exists.
     * @param  ftEnt  File table entry of the file whose size is sought.
     * @pre    ftEnt is not null.
     * @post   none.
     * @return The length of the specified file, in bytes, if successful; ERROR
     *          code otherwise.
     */
    public final int fsize(FileTableEntry ftEnt) {
        if (ftEnt == null) {
            return Kernel.ERROR;
        } // end if (ftEnt != null)

        return ftEnt.inode.length;
    } // end fsize(FileTableEntry)
    
    /**
     * read().
     * @param  ftEnt = fileTableEntry calling read.
     * @param  buffer = byte array acting as buffer for read.
     * @return number of bytes read.
     */
    public int read(FileTableEntry ftEnt, byte buffer[]) {
    	//if bad pointer, return error
    	if (ftEnt == null)
            return Kernel.ERROR;
    	int readLength = buffer.length; 				//total amount to read
    	int origSeekPtr = ftEnt.seekPtr;				//original seek pointer
    	
    	//if bufferLength is larger than total file length minus seek location,
    	//shorten total read length to match.
    	if((ftEnt.inode.length-origSeekPtr)<readLength)
    		readLength = ftEnt.inode.length-origSeekPtr;
    	
    	byte[] reader = new byte[512];
    	
    	//if reading single block of information.
    	//should add logic for case when read size is small and overlaps two blocks
    	//should also refactor this into single case below
    	if(readLength<=512){
    		int blockLoc = ftEnt.inode.findTargetBlock(origSeekPtr);
    		SysLib.rawread(blockLoc, reader);
        	System.arraycopy(reader, origSeekPtr%512, buffer, 0, readLength); //copy to buffer
        	return readLength;
    	}
    	
    	//if multiple blocks need to be read
    	else{
    		int bufferRead = 0;			//amount read so far
    		int nextReadLength = 0;		//amount to read this iteration
    		
    		//while length of buffer being read is less than inode length
    		//continue with next block address and add to buffer
    		while(bufferRead < readLength){
    			//butes left in file
    			int bytesLeft = readLength-ftEnt.seekPtr;
    			//if near end of file, shorten readLength
    			if((bytesLeft)<(512))
    	    		nextReadLength = bytesLeft;
    			else
    				nextReadLength = 512;
    			//find block on disk based on current file offset
    			int block = ftEnt.inode.findTargetBlock(bufferRead+origSeekPtr);
        		SysLib.rawread(block, reader);				//read from disk
        		//copy reader to current pointer in buffer
        		System.arraycopy(reader, 0, buffer, bufferRead, nextReadLength);
        		bufferRead += nextReadLength;
        		ftEnt.seekPtr += nextReadLength;	//advance seek ptr
    		}
    		return bufferRead;
    	}
    } // end read(int, byte[])
    
    
    /**
     * write().
     * @param  ftEnt = fileTableEntry reference calling write.
     * @param  buffer = buffer being written from.
     * @return number of bytes written during method.
     * writes buffer to inode referenced in ftEnt. Should refactor "direct pointer"
     * case and "indirect pointer" case into single block.
     */
    public int write(FileTableEntry ftEnt, byte buffer[]) {
        if (ftEnt == null) {
            return Kernel.ERROR;
        } // end if (ftEnt == null)
        
    	int fileLoc = ftEnt.seekPtr;
    	int origSeekPtr = fileLoc;
    	int buffSize = buffer.length;
    	int numOfBlocks;
    	//if starting at beginning of block, number of blocks has ratio vs buffSize
    	if (fileLoc % 512 == 0)
    		numOfBlocks = ((buffSize-1)/512)+1;
    	//if straddling blocks, use seekPtr location + buffSize to find numOfBlocks
    	else
    		numOfBlocks = ((buffSize + (origSeekPtr % 512)-1)/512)+1;
    	int nextBlockSize = 0;
    	int bufferWritten = 0;
    	String mode = ftEnt.mode;
    	
    	//while inside direct pointers
    	for(int i = 0; i<numOfBlocks && fileLoc < 11*512; i++){
    		
    		//if last block or piece of last block in buffer
    		if(i+1==numOfBlocks)
    			nextBlockSize = buffSize%512;
    		
    		//if writing somewhere other than start of a block
    		else if((fileLoc) % 512 != 0){
    			//if first block of buffer and buffer > 2 blocks
    			if(buffSize+origSeekPtr > 512)
    				nextBlockSize = 512-origSeekPtr;
    			//if only one block
    			else
    				nextBlockSize = buffSize;
    			}
    		//writing middle blocks
    		else
    			nextBlockSize = 512;
    		
    		byte[] writer = new byte[512];
    		//find block on disk based on current offset
    		short nextBlock = (short) ftEnt.inode.findTargetBlock(origSeekPtr+bufferWritten);
    		
    		//if outside existing boundary, register new block
    		if(mode.equals("w")|| nextBlock==-1){
    			nextBlock = (short) superblock.getFreeBlock();
    			ftEnt.inode.registerTargetBlock(nextBlockSize, nextBlock);
    		}
    		else{ // inside current boundary, read block
    			SysLib.rawread(nextBlock, writer);
    		}
    		
    		//copy segment of buffer to writer
        	System.arraycopy(buffer, bufferWritten, writer, (fileLoc)%512, nextBlockSize);
        	//write buffer segment to nextFreeBlock on disk
        	SysLib.rawwrite(nextBlock, writer);
        	
        	//increment pointers/accumulators
        	ftEnt.seekPtr += nextBlockSize;
        	bufferWritten += nextBlockSize;
        	ftEnt.inode.length += nextBlockSize;
        	//write inode to disk
        	ftEnt.inode.toDisk(ftEnt.iNumber);
        	fileLoc +=nextBlockSize;
    	}
    	if(bufferWritten==buffSize){			//if only direct blocks used
    		return bufferWritten;
    	}
    	else{ //register index block and fill with rest of buffer
    		int k = 0;
    		int typeFlag =0;
    		
    		//if no indirect block registered, register one with next free block
    		if(ftEnt.inode.indirect==-1){
    			short indexBlock = (short) superblock.getFreeBlock();
    			ftEnt.inode.registerIndexBlock(indexBlock);
    		}
    		
    		//while buffer.length is less than bufferWritten,
    		//continue writing to disk
    		while(bufferWritten < buffer.length){
    			//current seek pointer
    			fileLoc = ftEnt.seekPtr;
    			
    			// if last block or first small block
    			if (numOfBlocks-12 <= k){
    				//if data after direct nodes
    				if(numOfBlocks>11){
    					nextBlockSize = buffSize-bufferWritten;
    					typeFlag = 0;
    				}
    				//first node of data spanning two blocks
    				else if((fileLoc) % 512 + buffSize > 512){
    					nextBlockSize = 512-(fileLoc+512) % 512;
    					typeFlag = 1;
    				}
    				else{//second node of data spanning two small blocks
    					nextBlockSize = buffSize-bufferWritten;
    					typeFlag = 2;
    				}
    			}
        		else	//middle blocks
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
           			System.arraycopy(buffer,bufferWritten,writer,
           					(bufferWritten)%512,nextBlockSize);
           	//first block in condition where data starts mid block and overlaps.
           		else if(typeFlag ==1)
           			System.arraycopy(buffer,bufferWritten,writer,
           					origSeekPtr%512,nextBlockSize);
           	//first block
            		else
            			System.arraycopy(buffer,bufferWritten,writer,
            					0,nextBlockSize);
            		SysLib.rawwrite(nextBlock,writer);
            		
            		//increment accumulators and write inode to disk
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
     * seek().
     * @param  ftEnt
     * @param  offset
     * @param  whence
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
    	//normal condition: mark seekPointer to offset
    	case 0:
    		if(offset < fileLength && offset >= 0 ){
    			ftEnt.seekPtr=offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return Kernel.ERROR;
    	case 1:
    		//normal condition: mark seekPointer to offset + currentPointer
    		if(offset > 0 && offset <= fileLength - currentPtr){
    			ftEnt.seekPtr = currentPtr + offset;
    			return ftEnt.seekPtr;
    		}
    		//normal condition: mark seekPointer to currentPointer - offset
    		else if(offset<0 && (offset*-1) <= currentPtr){
    			ftEnt.seekPtr = currentPtr - offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return Kernel.ERROR;
    	//normal condition: mark seekPointer to fileLength - offset
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
     * delete().
     * @param  fileName representing file to be deleted.
     * @return true on success, false on failure.
     * opens a file with fileName in order to retrieve ftEnt referencing Inode.
     * Immediately closes ftEnt but retains reference. Sets Inode flag==-1 so
     * no other threads can open.
     * 
     * While other threads have file open, busy wait.
     * 
     * Once all other threads have closed, deallocate blocks, return iNumber to
     * freeList, and then write empty inode to disk.
     */
    public boolean delete(String fileName) {
        FileTableEntry ftEnt = open(fileName, "r");
        close(ftEnt);
        ftEnt.inode.flag = -1;
        while(ftEnt.inode.count > 0)
            ;
        deallocAllBlocks(ftEnt);
        directory.ifree(ftEnt.iNumber);
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    } // end delete(String)
    
    
    /**
     * Writes file system information to disk if there are no threads accessing
     *  it.
     * @pre    No threads are accessing files in this file system.
     * @post   The superblock and directory have been written to persistent
     *          storage.
     * @return true is the file table is empty and the disk write proceeded;
     *          false otherwise.
     */
    public boolean sync() {
        if (!filetable.fempty()) {
            return false;
        } // end if (!filetable.fempty())
        
        superblock.sync();
        FileTableEntry dirEnt = open("/", "w");
        write(dirEnt, directory.directory2bytes());
        close(dirEnt);
        
        return true;
    } // end sync()
    
    
    /**
     * deallocAllBlocks().
     * @param  ftEnt = fileTableEntry being deallocated.
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if(ftEnt == null)
            return false;
        //deallocate all direct blocks
        for(int i = 0; i<11; i++){
            if(ftEnt.inode.direct[i]==-1)
                return true;
            if(!superblock.returnBlock(i))
                return false;
        }
        
        //unregister indexBlock of Inode,
        //receiving byte array of block locations
        byte[] freeBlocks = ftEnt.inode.unregisterIndexBlock();
        
        //deallocate all indirect blocks
        for (int i = 0; i<ftEnt.inode.length/512-11; i++){
            int blockNum = (int) freeBlocks[i*2];
            if(!superblock.returnBlock(blockNum))
                return false;
        }
        return true;
    } // end deallocAllBlocks(FileTableEntry)
} // end class FileSystem
