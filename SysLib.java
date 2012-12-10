import java.util.*;

public class SysLib {
    public static int exec( String args[] ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXEC, 0, args );
    }

    public static int join( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WAIT, 0, null );
    }

    public static int boot( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.BOOT, 0, null );
    }

    public static int exit( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.EXIT, 0, null );
    }

    public static int sleep( int milliseconds ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SLEEP, milliseconds, null );
    }

    public static int disk( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_DISK,
				 0, 0, null );
    }

    public static int cin( StringBuffer s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.READ, 0, s );
    }

    public static int cout( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 1, s );
    }

    public static int cerr( String s ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, 2, s );
    }

    public static int rawread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWREAD, blkNumber, b );
    }

    public static int rawwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.RAWWRITE, blkNumber, b );
    }

    public static int sync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SYNC, 0, null );
    }

    public static int cread( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CREAD, blkNumber, b );
    }

    public static int cwrite( int blkNumber, byte[] b ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CWRITE, blkNumber, b );
    }

    public static int flush( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CFLUSH, 0, null );
    }

    public static int csync( ) {
        return Kernel.interrupt( Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CSYNC, 0, null );
    }

    public static String[] stringToArgs( String s ) {
        StringTokenizer token = new StringTokenizer( s," " );
        String[] progArgs = new String[ token.countTokens( ) ];
        for ( int i = 0; token.hasMoreTokens( ); i++ ) {
            progArgs[i] = token.nextToken( );
        }
        return progArgs;
    }

    public static void short2bytes( short s, byte[] b, int offset ) {
        b[offset] = (byte)( s >> 8 );
        b[offset + 1] = (byte)s;
    }

    public static short bytes2short( byte[] b, int offset ) {
        short s = 0;
            s += b[offset] & 0xff;
        s <<= 8;
            s += b[offset + 1] & 0xff;
        return s;
    }

    public static void int2bytes( int i, byte[] b, int offset ) {
        b[offset] = (byte)( i >> 24 );
        b[offset + 1] = (byte)( i >> 16 );
        b[offset + 2] = (byte)( i >> 8 );
        b[offset + 3] = (byte)i;
    }

    public static int bytes2int( byte[] b, int offset ) {
        int n = ((b[offset] & 0xff) << 24) + ((b[offset+1] & 0xff) << 16) +
                ((b[offset+2] & 0xff) << 8) + (b[offset+3] & 0xff);
        return n;
    }
    
    
    /* Added by Brendan Sweeney */
    
    /**
     * Reads up to buffer.length bytes from the file associated with file
     *  descriptor fd, starting at the current position of the seek pointer. If
     *  bytes remaining between the current seek pointer and the end of file
     *  are less than buffer.length, as many bytes as possible are read,
     *  putting them into the beginning of buffer. The seek pointer is
     *  incremented by the number of bytes that have been read. The return
     *  value is the number of bytes that have been read, or a negative value
     *  upon an error.
     * @param  fd  File descriptor of the file to read.
     * @param  buffer  A buffer into which bytes read from the file are placed.
     * @pre    The file described by fd is open.
     * @post   buffer contains all bytes from the file's original seek pointer
     *          up to either buffer.length or the end of the file; seek pointer
     *          is set to one past the last byte read.
     * @return The number of bytes read if successful; -1 otherwise.
     */
    public static int read(int fd, byte[] buffer) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.READ, fd, buffer);
    } // end delete(String)
    
    
    /**
     * Writes the contents of buffer to the file associated with the file
     *  descriptor fd, starting at the current position of the seek pointer.
     *  The operation may overwrite existing data in the file and/or append to
     *  the end of the file. The seek pointer is incremented by the number of
     *  bytes to have been written. The return value is the number of bytes
     *  that have been written, or a negative value upon an error.
     * @param  fd  File descriptor of the file to write into.
     * @param  buffer  A buffer containing the bytes to be written to the file.
     * @pre    The file described by fd is open; there are enough free blocks
     *          on the disk to hold the written bytes.
     * @post   All bytes in buffer have been written to the file starting from
     *          its original seek pointer; seek pointer is set to one past the
     *          last byte written.
     * @return The number of bytes written if successful; -1 otherwise.
     */
    public static int write(int fd, byte[] buffer) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.WRITE, fd, buffer);
    } // end delete(String)
    
    
    /**
     * Opens the file specified by the fileName string in the given mode and
     *  allocates a new int file descriptor to reference this file. For modes
     *  "w", "w+" or "a", the file is created if it does not exist; for mode
     *  "r", an error results if the file does not exist. The seek pointer is
     *  positioned at the beginning of the file in modes "r", "w", and "w+"; it
     *  is positioned at the end of the file in mode "a". File descriptors 3
     *  thru 31 are available for user files. If the calling thread's user file
     *  descriptor table is full, an error is returned; otherwise the new file
     *  descriptor is returned.
     * @param  fileName  The name of the file to open.
     * @param  mode  Access mode of the file. May be "r" for read-only, "w" for
     *                write-only, "w+" for read-and-write, or "a" for append.
     * @pre    mode specifies a valid access mode; if mode is "r" then fileName
     *          specifies an existing file; the caller's file descriptor table
     *          is not full.
     * @post   The specified file is added to the file table and is accessible
     *          in the specified mode through the returned file descriptor.
     * @return A file descriptor if the file could be opened; -1 otherwise.
     */
    public static int open(String fileName, String mode) {
        String[] args = new String[2];
        args[0] = fileName;
        args[1] = mode;
        
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.OPEN, 0, args);
    } // end open(String, String)
    
    
    /**
     * Closes the file corresponding to fd, commits all file transactions on
     *  this file, and unregisters fd from the user file descriptor table of
     *  the calling thread's TCB.
     * @param  fd  File descriptor of the file to close.
     * @pre    The file described by fd is open.
     * @post   All transactions on this file are committed; fd is unregistered
     *          from the caller's file descriptor table.
     * @return 0 if the file was found and closed; -1 otherwise.
     */
    public static int close(int fd) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.CLOSE, fd, null);
    } // end close(int)
    
    
    /**
     * Returns the size in bytes of the file indicated by fd.
     * @param  fd  File descriptor of the file whose size is requested.
     * @pre    The file described by fd is open.
     * @post   None.
     * @return The size, in bytes, of the file if found; -1 otherwise.
     */
    public static int fsize(int fd) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SIZE, fd, null);
    } // end fsize(int)
    
    
    /**
     * Updates the seek pointer corresponding to fd depending on the value of
     *  whence and offset.
     * @param  fd  File descriptor of the file to seek into.
     * @param  offset  Distance to set the seek pointer from whence. May be
     *                  negative to specify a distance before whence.
     * @param  whence  Location in the file from which offset should start. 0
     *                  is the start of the file, 1 is the current seek pointer
     *                  position, and 2 is the end of the file.
     * @pre    If whence is 0, then offset is positive; if whence is 2, then
     *          offset is negative; whence plus offset is within the bounds of
     *          the file.
     * @post   The file's seek pointer is set to the specified location.
     * @return The new seek pointer position if set; -1 otherwise.
     */
    public static int seek(int fd, int offset, int whence) {
        int[] args = new int[2];
        args[0] = offset;
        args[1] = whence;
        
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.SEEK, fd, args);
    } // end seek(int, int, int)
    
    
    /**
     * Formats the disk, (i.e., Disk's data contents). The parameter files
     *  specifies the maximum number of files to be created, (i.e., the number
     *  of inodes to be allocated) in your file system. The return value is 0
     *  on success, otherwise -1.
     * @param  files  The maximum number of files to support.
     * @pre    The disk file has been created.
     * @post   The disk file contains a file system that supports up to the
     *          specified number of files.
     * @return 0 if the format succeeded; -1 otherwise.
     */
    public static int format(int files) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.FORMAT, files, null);
    } // end format(int)
    
    
    /**
     * Deletes the file specified by fileName. If the file is currently open,
     *  it is not deleted until the last open on it is closed, but new attempts
     *  to open it will fail.
     * @param  fileName  The name of the file to delete.
     * @pre    fileName specifies a file that exists.
     * @post   The file is removed from the file system directory, its inode is
     *          made available, and its blocks are returned to the free list.
     * @return 0 if the file existed and was deleted; -1 otherwise.
     */
    public static int delete(String fileName) {
        return Kernel.interrupt(Kernel.INTERRUPT_SOFTWARE,
				 Kernel.DELETE, 0, fileName);
    } // end delete(String)
}
