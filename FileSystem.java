/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author brendan
 */
public class FileSystem {
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    private SuperBlock blockZero;
    
    
    public int format(int files) {
        return 0;
    } // end format(int)
    
    
    public int open(String fileName, String mode) {
        return -1;
    } // end open(String, String)
    
    
    public int read(int fd, byte buffer[]) {
        return buffer.length;
    } // end read(int, byte[])
    
    
    public int write(int fd, byte buffer[]) {
        return buffer.length;
    } // end write(int, byte[])
    
    
    public int seek(int fd, int offset, int whence) {
        return 0;
    } // end seek(int, int, int)
    
    
    public int close(int fd) {
        return 0;
    } // end close(int)
    
    
    public int delete(String fileName) {
        return 0;
    } // end delete(String)
    
    
    public int fsize(int fd) {
        return 0;
    } // end fsize(int)
} // end class FileSystem
