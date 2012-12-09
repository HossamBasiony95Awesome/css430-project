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
    	superblock.format(DEFAULT_BLOCKS);
        superblock.freeList += files / inodesPerBlock;
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
        	System.arraycopy(reader, 0, buffer, 0, readLength); //copy to buffer
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
    	if(fileLoc == 0){ 							//if mode = w or w+
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
    		}
    		if(numOfBlocks<12){						//if only direct blocks used
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
            		k++;
        		}
    			return buffer.length;
    		}
    	}
    	else
    		//need to write case for mode== 'a' and fileLoc !=0
        return buffer.length;
    } // end write(int, byte[])
    
    
    /**
     * .
     * @param  ftEnt  .
     * @param  offset  .
     * @param  whence  .
     * @pre    .
     * @post   .
     * @return .
     */
    public int seek(FileTableEntry ftEnt, int offset, int whence) {
        return 0;
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
            return false;
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
