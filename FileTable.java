/*
 * @file    FileTbale.java
 * @brief   This class is the FileTable structure for a simple filesystem.
 * @author  Chris Grass
 * @date    December 12, 2012
 */
import java.util.Vector;


public class FileTable {
	  private Vector<FileTableEntry> table;         // the actual entity of this file table
	  private Directory dir;        // the root directory 

	    /** 
	     * Constructor
	     * @param  dir  .
	     * @pre    .
	     * @post   .
	     * constructs a FileTable
	     */
	  public FileTable( Directory directory ) { // constructor
	    table = new Vector<FileTableEntry>();       // instantiate a file table
	    dir = directory;            // receive a reference to the Director
	  }                             // from the file system
	  
	    /** 
	     * falloc
	     * @param  String filename, String mode  .
	     * @pre    .
	     * @post   .
	     * @return returns reference to new FileTableEntry; null if error
	     * creates a FileTableEntry based on filename and mode.
	     */
	  public synchronized FileTableEntry falloc( String filename, String mode ) {

		  // retrieve iNum from dir for corresponding filename. -1 if none exists
		  short iNum = -1;
		  Inode inode = null;
		  
		  //busy loop
		  while (true){
              iNum = dir.namei( filename );
			  if(iNum<0){						//if new file, create Inode
				  if(mode.compareTo("r")==0)	//if no file exists and trying to read
					  return null;
				  inode = new Inode();
				  iNum = dir.ialloc(filename);	//allocate iNum from freeList
			  }
			  else
				  inode = new Inode(iNum);		//push existing Inode to memory
			  if(inode.flag==-1)
                      return null;
			  if(mode.compareTo("r")==0){		//if read-only, check if flag
				  if(inode.flag!=3){			//is set to writing(3)
					  inode.flag=2;				//if so, wait for flag to clear
					  break;					
				  }
			  }
			  //	  mode is w, w+, or a
			  else{				  
				  if(inode.flag < 2 ){			//is set to writing(3)
					  inode.flag = 3;
					  break;					//if so, wait for flag to clear
				  }
			  }
				  
		  }
		  
		  // allocate a new file table entry for this file name
		  FileTableEntry newEntry = new FileTableEntry(inode,iNum,mode);
		  table.add(newEntry);					//add newEntry to table
		  // increment this inode's count
		  inode.count++;						//increment inode's count
		  // immediately write back this inode to the disk
		  inode.toDisk(iNum);					//save updated inode to disk
		  // return a reference to this file table entry
		  return newEntry;
	  }

	    /** 
	     * ffree
	     * @param  FileTableEntry e .
	     * @pre    .
	     * @post   .
	     * @return true on success, false if e doesn't exist in table.
	     * removes FileTableEntry from table and decrements inode.count
	     */
	  public synchronized boolean ffree( FileTableEntry e ) {
	    // receive a file table entry reference
		  int loc = findLoc(e); 			//find index of entry
		  if (loc<0)						//if table doesn't contain e
			  return false;					//return false
		  e.inode.count--;
                  if (e.inode.count == 0)
                      e.inode.flag = 0;
		  e.inode.toDisk(e.iNumber);		//save updated inode to disk
		  return table.remove(table.elementAt(loc));   //remove e from table
	  }
	  
	    /** 
	     * fempty
	     * @param  .
	     * @pre    .
	     * @post   .
	     * test for empty table
	     */
	  public synchronized boolean fempty() {
	    return table.isEmpty();  // return if table is empty 
	  }                          // should be called before starting a format
	  
	    /** 
	     * findLoc
	     * @param  FileTableEntry entry
	     * @pre    .
	     * @post   .
	     * finds and returns index of entry. returns -1 if not found
	     */
	  private int findLoc(FileTableEntry entry){
		  int index = -1;
		  for(int i = 0; i <table.size(); i++){
			  if(table.elementAt(i).equals(entry))
				  return i;
		  }
		  return index;//-1 if error or not found
	  }

          public FileTableEntry findFtEnt(short iNum){
              for (int i = 0; i<table.size(); i++)
                  if(table.elementAt(i).iNumber == iNum)
                     return table.elementAt(i);
              return null;
          }
}

