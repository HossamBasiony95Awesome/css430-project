/*
 * @file    FileSystem.java
 * @brief   .
 * @author  Brendan Sweeney, SID 1161836
 * @date    December 12, 2012
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
        
        superblock.totalInodes = files;
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
    public FileTableEntry open(String filename, String mode) {
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
     * @return -1 on error.
     */
    public boolean close(FileTableEntry ftEnt) {
        ftEnt.inode.count--;
    	if(filetable.ffree(ftEnt)==false)
    		return false;
        return true;
    } // end close(int)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int fsize(FileTableEntry ftEnt) {
        return 0;
    } // end fsize(int)
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  buffer  .
     * @pre    .
     * @post   .
     * @return buffer.length.
     */
    public int read(FileTableEntry ftEnt, byte buffer[]) {
    	int readLength = buffer.length;
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
    	//need to implement 
//    	else{							//continue reading from indirect array
//    		int j = 0;
//    		//while length of buffer being read is less than inode length
//    		//continue with next indirect address and add to buffer
//    		while(buffer.length < ftEnt.inode.length){
//    			int block = ftEnt.inode.findTargetBlock(buffer.length);
//        		SysLib.rawread(block, reader);				//read from disk
//        		//copy reader to end of buffer
//        		System.arraycopy(reader, 0, buffer, buffer.length, 512); 
//        		j++;
//    		}
//    		return buffer.length;
//    	}
    	return readLength;
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
    	if(fileLoc == 0){ 						//if writing to start of file
    		ftEnt.inode.length = 0;
    		for(int i = 0; i<numOfBlocks; i++){		//for direct nodes    
    			if(i+1==numOfBlocks)				//last block
    				nextBlockSize = buffSize%512;
    			else
    				nextBlockSize = 512;
    			short nextFreeBlock = (short) superblock.getFreeBlock();
    			ftEnt.inode.registerTargetBlock(nextBlockSize, nextFreeBlock);
    			byte[] writer = new byte[512];
    			//copy segment of buffer to writer
        		System.arraycopy(buffer, i*512, writer, 0, nextBlockSize);
        		//write buffer segment to nextFreeBlock on disk
        		SysLib.rawwrite(nextFreeBlock, writer);
                ftEnt.inode.length += nextBlockSize;
    		}
    		if(numOfBlocks<12){						//if only direct blocks used
                ftEnt.inode.toDisk(ftEnt.iNumber);
    			return buffer.length;
    		}
    		else{ //register index block and fill with rest of buffer
    			int k = 0;
    			//while Inode.length is less than buffer.length, continue
    			//writing to disk    			
    			while(ftEnt.inode.length < buffer.length ){
    				int currentInodeLength = ftEnt.inode.length;
    				if (numOfBlocks-12 > k)				//if not last block
    					nextBlockSize = currentInodeLength+512;
        			else								//add remainder to length
        				nextBlockSize = currentInodeLength%512;
        			short nextFreeBlock = (short) superblock.getFreeBlock();
    				//register new indirect block at offset=inode.length
    				ftEnt.inode.registerTargetBlock(nextBlockSize, 
    						nextFreeBlock);
            		byte[] writer = new byte[512];
            		System.arraycopy(buffer,currentInodeLength,writer,0,nextBlockSize);
            		SysLib.rawwrite(nextFreeBlock,writer);
                    ftEnt.inode.toDisk(ftEnt.iNumber);
            		k++;
        		}
    			return buffer.length;
    		}
    	}
    	else
            return append( ftEnt, buffer);
    } // end write(int, byte[])
    
    private int append(FileTableEntry ftEnt, byte buffer[]){
       int fileLoc = ftEnt.seekPtr;
       int buffSize = buffer.length;
       int numOfBlocks = buffSize/512+1;
       boolean blockOverlap = false;
       int writeLength = fileLoc+buffSize;;
       int lastInodeBlock = ftEnt.inode.findTargetBlock(buffSize+fileLoc);
       if((fileLoc+buffSize)>512)
               blockOverlap = true;
       if(blockOverlap == false){              //simple case appending to single block
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
    	int currentPtr = ftEnt.seekPtr;
    	int fileLength = ftEnt.inode.length;
    	switch(whence){
    	case 0:
    		if(offset < fileLength && offset > 0 ){
    			ftEnt.seekPtr=offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return -1;
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
    			return -1;
    	case 2:
    		if(offset < 0 && offset < fileLength){
    			ftEnt.seekPtr = fileLength + offset;
    			return ftEnt.seekPtr;
    		}
    		else
    			return -1;
    	default:
    		return -1;
    	}
    } // end seek(int, int, int)
    
    
    /**
     * .
     * @param  fileName  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int delete(String fileName) {
        return 0;
    } // end delete(String)
    
    
    /**
     * .
     * @param  fileName  .
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
        return false;
    } // end deallocAllBlocks(FileTableEntry)
//    
//    
//    /**
//     * .
//     * @param  data  .
//     * @pre    .
//     * @post   .
//     * @return .
//     */
//    private void initSuperblock() {
//        int free  = DEFAULT_FILES * (Disk.blockSize / Inode.iNodeSize) + 2;
//        superblock = new SuperBlock();
//    } // end initSuperblock()
//    
//    
//    /**
//     * .
//     * @param  data  .
//     * @pre    .
//     * @post   .
//     * @return .
//     */
//    private void initInodes() {
//        Inode current;
//        inodes = new Vector<Inode>();
//        
//        for (short i = 0; i < superblock.totalInodes; ++i) {
//            current = new Inode(i);
//            inodes.add(current);
//        } // end for (; i < blockZero.totalInodes; )
//    } // end initInodes()
} // end class FileSystem
