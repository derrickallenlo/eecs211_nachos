package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess
{

    protected Coff coff;                    //The program being run by this process.
    protected TranslationEntry[] pageTable; //This process's page table.
    protected int numPages;                 //The number of contiguous pages occupied by the program.
    protected final int stackPages = 8;     //The number of pages in the program's stack.

    private int initialPC, initialSP;
    private int argc, argv;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    //project2 partI variables
    private OpenFile fileDescribtors[];//tracking file descriptors
    
    /**
     * Allocate a new process.
     */
    public UserProcess()
    {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i = 0; i < numPhysPages; i++)
        {
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
        }
        
        //project2 partI variables initializer 
        fileDescribtors = new OpenFile[16];//16 concurrent files include stdin and stdout
        fileDescribtors[0] = UserKernel.console.openForReading();//stdin    
        fileDescribtors[1] = UserKernel.console.openForWriting();//stdout    
    }

    /**
     * Allocate and return a new process of the correct class. The class name is
     * specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess()
    {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args)
    {
        if (!load(name, args))
        {
            return false;
        }

        new UThread(this).setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState()
    {
        //TODO ???
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState()
    {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read at
     * most <tt>maxLength + 1</tt> bytes from the specified address, search for
     * the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr the starting virtual address of the null-terminated string.
     * @param maxLength the maximum number of characters in the string, not
     * including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength)
    {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++)
        {
            if (bytes[length] == 0)
            {
                return new String(bytes, 0, length);
            }
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data)
    {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no data
     * could be copied).
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to the
     * array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length)
    {
        Lib.assertTrue(offset >= 0 && length >= 0
                       && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
        {
            return 0;
        }

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(memory, vaddr, data, offset, amount);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data)
    {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no data
     * could be copied).
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to virtual
     * memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length)
    {
        Lib.assertTrue(offset >= 0 && length >= 0
                       && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
        {
            return 0;
        }

        int amount = Math.min(length, memory.length - vaddr);
        System.arraycopy(data, offset, memory, vaddr, amount);

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args)
    {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null)
        {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try
        {
            coff = new Coff(executable);
        }
        catch (EOFException e)
        {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++)
        {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages)
            {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++)
        {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize)
        {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
        {
            return false;
        }

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++)
        {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]
            {
                0
            }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be run
     * (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections()
    {
        if (numPages > Machine.processor().getNumPhysPages())
        {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++)
        {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                                  + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++)
            {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, vpn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections()
    {
        //TODO ???
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of the
     * stack, set the A0 and A1 registers to argc and argv, respectively, and
     * initialize all other registers to 0.
     */
    public void initRegisters()
    {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < Processor.numUserRegisters; i++)
        {
            processor.writeRegister(i, 0);
        }

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Halt the Nachos machine by calling Machine.halt(). Only the root process
     * (the first process, executed by UserKernel.run()) should be allowed to
     * execute this syscall. Any other process should ignore the syscall and
     * return immediately.
     */
    private int handleHalt()
    {
        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    /**
     * Terminate the current process immediately. Any open file descriptors
     * belonging to the process are closed. Any children of the process no
     * longer have a parent process.
     *
     * status is returned to the parent process as this process's exit status
     * and can be collected using the join syscall. A process exiting normally
     * should (but is not required to) set status to 0.
     *
     * exit() never returns.
     */
    private int handleExit(int status)
    {
        //TODO implement me

        return 0;   //void
    }

    /**
     * Execute the program stored in the specified file, with the specified
     * arguments, in a new child process. The child process has a new unique
     * process ID, and starts with stdin opened as file descriptor 0, and stdout
     * opened as file descriptor 1.
     *
     * file is a null-terminated string that specifies the name of the file
     * containing the executable. Note that this string must include the ".coff"
     * extension.
     *
     * argc specifies the number of arguments to pass to the child process. This
     * number must be non-negative.
     *
     * argv is an array of pointers to null-terminated strings that represent
     * the arguments to pass to the child process. argv[0] points to the first
     * argument, and argv[argc-1] points to the last argument.
     *
     * exec() returns the child process's process ID, which can be passed to
     * join(). On error, returns -1.
     */
    private int handleExec(char name, int argc, char argv1, char argv2)     //TODO not sure if this is right # of params
    {
        //TODO implement me

        return -1;   //TODO return ???
    }

    /**
     * Suspend execution of the current process until the child process
     * specified by the processID argument has exited. If the child has already
     * exited by the time of the call, returns immediately. When the current
     * process resumes, it disowns the child process, so that join() cannot be
     * used on that process again.
     *
     * processID is the process ID of the child process, returned by exec().
     *
     * status points to an integer where the exit status of the child process
     * will be stored. This is the value the child passed to exit(). If the
     * child exited because of an unhandled exception, the value stored is not
     * defined.
     *
     * If the child exited normally, returns 1. If the child exited as a result
     * of an unhandled exception, returns 0. If processID does not refer to a
     * child process of the current process, returns -1.
     */
    private int handleJoin(int pid, int status)
    {
        //TODO implement me

        return -1;   //TODO return ???
    }

    /**
     * Attempt to open the named disk file, creating it if it does not exist,
     * and return a file descriptor that can be used to access the file.
     *
     * Note that creat() can only be used to create files on disk; creat() will
     * never return a file descriptor referring to a stream.
     *
     * Returns the new file descriptor, or -1 if an error occurred.
     */
    private int handleCreate(char name)
    {
		Lib.debug(dbgProcess, "\t At handleCreate");
    	if (name < 0)
    	{
    		Lib.debug(dbgProcess, "\t Negative virtual address name detected");
    		return -1;
    	}
    	
    	//User.Process.readVirtualMemory used transfer between the user process and kernel
    	//with system calls is 256 bytes
    	String fileName = readVirtualMemoryString (name, 256);
    	if (fileName == null)
    	{
    		Lib.debug(dbgProcess, "\t Null file name detected");
    		return -1;
    	}
    	
    	//found FileDecriptorIndex of supporting max 16 concurrent files per user program
    	int firstAvailableIndex = -1;
    	for (int fdIndex = 0; fdIndex < 16; fdIndex++)
    	{
    		//scan all 16 spaces - not creating if it does exist
    		if ((fileDescribtors[fdIndex] != null) && (fileDescribtors[fdIndex].getName().equals(fileName)) )
    		{
    			Lib.debug(dbgProcess, "\t The same file name detected");
    			return -1;
    		}
    		
    		if ((fileDescribtors[fdIndex] == null) && (firstAvailableIndex < 0))//creating it if it does not exist
    		{
    			firstAvailableIndex = fdIndex;
    		}
    	}
    	
    	if (firstAvailableIndex > 0)
    	{
    		Lib.debug(dbgProcess, "\tCreating File...");
			fileDescribtors[firstAvailableIndex] = ThreadedKernel.fileSystem.open(fileName, true);
			return firstAvailableIndex;
    	}
		
    	Lib.debug(dbgProcess, "\tFile Descriptor has reached the maxium - 16 concurrent files");
        return -1;
    }

    /**
     * Attempt to open the named file and return a file descriptor.
     *
     * Note that open() can only be used to open files on disk; open() will
     * never return a file descriptor referring to a stream.
     *
     * Returns the new file descriptor, or -1 if an error occurred.
     */
    private int handleOpen(char name)
    {
        Lib.debug(dbgProcess, "\t At handleOpen");
    	if (name < 0)
    	{
    		Lib.debug(dbgProcess, "\t Negative virtual address name detected");
    		return -1;
    	}
    	
    	//User.Process.readVirtualMemory used transfer between the user process and kernel
    	//with system calls is 256 bytes
    	String fileName = readVirtualMemoryString (name, 256);
    	if (fileName == null)
    	{
    		Lib.debug(dbgProcess, "\t Null file name detected");
    		return -1;
    	}
    	
    	//find it from array of file decribtors 
    	for (int fdIndex = 0; fdIndex < 16; fdIndex++)
    	{
    		if (fileDescribtors[fdIndex] != null && fileDescribtors[fdIndex].getName().equals(fileName))
    		{
    			ThreadedKernel.fileSystem.open(fileName, false);//not making the new file set to false
    			return fdIndex;
    		}
    	}
    	Lib.debug(dbgProcess, "\t File"+fileName+"not found");
        return -1;  
    }
    
    /**
     * Attempt to read up to count bytes into buffer from the file or stream
     * referred to by fileDescriptor.
     *
     * On success, the number of bytes read is returned. If the file descriptor
     * refers to a file on disk, the file position is advanced by this number.
     *
     * It is not necessarily an error if this number is smaller than the number
     * of bytes requested. If the file descriptor refers to a file on disk, this
     * indicates that the end of the file has been reached. If the file
     * descriptor refers to a stream, this indicates that the fewer bytes are
     * actually available right now than were requested, but more bytes may
     * become available in the future. Note that read() never waits for a stream
     * to have more data; it always returns as much as possible immediately.
     *
     * On error, -1 is returned, and the new file position is undefined. This
     * can happen if fileDescriptor is invalid, if part of the buffer is
     * read-only or invalid, or if a network stream has been terminated by the
     * remote host and no more data is available.
     */
    private int handleRead(int fd, char buffer, int size)
    {
    	int numberBytes = 0;//number of bytes read 
    	
    	//fileDescriptor is invalid
    	if ((fd < 0) || (fd > 15))
    	{
    		Lib.debug(dbgProcess, "\t Invalid FileDecriptor index");
    		return -1;
    	}
    	
    	//count bytes - size - is invalid
    	if (size < 0)
    	{
    		Lib.debug(dbgProcess, "\t Invalid size of reading count bytes");
    		return -1;
    	}
    	else if (size == 0)
    	{
    		Lib.debug(dbgProcess, "\t 0 reading count bytes detected");
    		return 0;
    	}
    	
    	if (fileDescribtors[fd] == null)
    	{
    		Lib.debug(dbgProcess, "\t Invalid file decribtor");
    		return -1;
    	}
    	
    	OpenFile file = fileDescribtors[fd];
    	if (file == null)
    	{
    		Lib.debug(dbgProcess, "\t FileDecriptor is not found in the opened files");
    		return -1;
    	}
    	
    	//variables for file.read
    	/* read start from current file pointer
    	 * buf - the buffer to store the bytes in.
    	 * offset - the offset in the buffer to start storing bytes.
    	 * length - the number of bytes to read.
    	 * */
    	byte byteBuffer[] = new byte[size];//read up to count bytes into buffer
    	int pos = 0;
    	int offset = 0;
    	int length = size;//read up to count bytes into buffer
    	numberBytes = file.read(byteBuffer, offset, length);
    	
    	if ((numberBytes < 0) || (numberBytes > size))
    	{
    		Lib.debug(dbgProcess, "\t read file error");
    		return -1;
    	}
    	
    	/**
		 * @param	vaddr	the first byte of virtual memory to write.
		 * @param	data	the array containing the data to transfer.
		 * @param	offset	the first byte to transfer from the array.
		 * @param	length	the number of bytes to transfer from the array to
		 *			virtual memory.
		 * @return	the number of bytes successfully transferred.
		*/
    	numberBytes = writeVirtualMemory(buffer, byteBuffer, offset, numberBytes);
    	
    	if ((numberBytes < 0) || (numberBytes > size))
    	{
    		Lib.debug(dbgProcess, "\t write to virtual memory error");
    		return -1;
    	}
    		
        return numberBytes;  
    }

    /**
     * Attempt to write up to count bytes from buffer to the file or stream
     * referred to by fileDescriptor. write() can return before the bytes are
     * actually flushed to the file or stream. A write to a stream can block,
     * however, if kernel queues are temporarily full.
     *
     * On success, the number of bytes written is returned (zero indicates
     * nothing was written), and the file position is advanced by this number.
     * It IS an error if this number is smaller than the number of bytes
     * requested. For disk files, this indicates that the disk is full. For
     * streams, this indicates the stream was terminated by the remote host
     * before all the data was transferred.
     *
     * On error, -1 is returned, and the new file position is undefined. This
     * can happen if fileDescriptor is invalid, if part of the buffer is
     * invalid, or if a network stream has already been terminated by the remote
     * host.
     */
    private int handleWrite(int fd, char buffer, int size)//buffer is virtual address
    {
    	int numberBytes = 0;//number of bytes read 
    	
    	//fileDescriptor is invalid
    	if ((fd < 0) || (fd > 15))
    	{
    		Lib.debug(dbgProcess, "\t Invalid FileDecriptor index");
    		return -1;
    	}
    	
    	//count bytes - size - is invalid
    	if (size < 0)
    	{
    		Lib.debug(dbgProcess, "\t Invalid size of reading count bytes");
    		return -1;
    	}
    	else if (size == 0)
    	{
    		Lib.debug(dbgProcess, "\t 0 reading count bytes detected");
    		return 0;
    	}
    	
    	OpenFile file = fileDescribtors[fd];
    	if (file == null)
    	{
    		Lib.debug(dbgProcess, "\t FileDecriptor is not found in the opened files");
    		return -1;
    	}
    	
    	
    	//variables for readVirtualMemory from  memory to byteBuffer
    	/* @param vaddr the first byte of virtual memory to read.
         * @param data the array where the data will be stored.
         * @param offset the first byte to write in the array.
         * @param length the number of bytes to transfer from virtual memory to the
         * array.
         * @return the number of bytes successfully transferred.
         * */
    	byte byteBuffer[] = new byte[size];//read up to count bytes into buffer
    	int offset = 0;
    	int length = size;//read up to count bytes into buffer
    	numberBytes = readVirtualMemory(buffer, byteBuffer, offset, length);//buffer is address
    	
    	if ((numberBytes < 0) || (numberBytes > size))
    	{
    		Lib.debug(dbgProcess, "\t read data from virtual memory error");
    		return -1;
    	}
    		
    	
    	//variables for file.write
    	/*
		* Write this file starting at the current file pointer and return the
		* number of bytes successfully written. Advances the file pointer by this
		* amount. If no bytes could be written because of a fatal error, returns
		-1.
		* 
		* @param buf the buffer to get the bytes from.
		* @param offset the offset in the buffer to start getting.
		* @param length the number of bytes to write.
		@return the actual number of bytes successfully written, or -1 on
		* failure.
		*/
    	numberBytes = file.write(byteBuffer, offset, numberBytes);
    	
    	if ((numberBytes < 0) || (numberBytes > size))
    	{
    		Lib.debug(dbgProcess, "\t write file error");
    		return -1;
    	}
    	
        return numberBytes;  
    }

    /**
     * Close a file descriptor, so that it no longer refers to any file or
     * stream and may be reused.
     *
     * If the file descriptor refers to a file, all data written to it by
     * write() will be flushed to disk before close() returns. If the file
     * descriptor refers to a stream, all data written to it by write() will
     * eventually be flushed (unless the stream is terminated remotely), but not
     * necessarily before close() returns.
     *
     * The resources associated with the file descriptor are released. If the
     * descriptor is the last reference to a disk file which has been removed
     * using unlink, the file is deleted (this detail is handled by the file
     * system implementation).
     *
     * Returns 0 on success, or -1 if an error occurred.
     */
    private int handleClose(int fd)
    {
    	if ((fd < 0) || (fd > 15))
    	{
    		Lib.debug(dbgProcess, "\t Invalid FileDecriptor index");
    		return -1;
    	}
    	
    	if (fileDescribtors[fd] == null)
    	{
    		Lib.debug(dbgProcess, "\t NULL FileDecriptor found");
    		return -1;
    	}
    	
    	fileDescribtors[fd].close();
    	fileDescribtors[fd] = null;
    	Lib.debug(dbgProcess, "\t file closed succesfully");
    	
        return 0;
    }

    /**
     * Delete a file from the file system. If no processes have the file open,
     * the file is deleted immediately and the space it was using is made
     * available for reuse.
     *
     * If any processes still have the file open, the file will remain in
     * existence until the last file descriptor referring to it is closed.
     * However, creat() and open() will not be able to return new file
     * descriptors for the file until it is deleted.
     *
     * Returns 0 on success, or -1 if an error occurred.
     */
    private int handleUnlink(int addr)
    {
        if (addr < 0)
        {
        	Lib.debug(dbgProcess, "\t Invalid address - unlink");
    		return -1;
        }
        
        String fileName = readVirtualMemoryString(addr, 256);
        if (fileName == null)
        {
        	Lib.debug(dbgProcess, "\t Invalid File Name - unlink");
    		return -1;
        }
        
    	if(ThreadedKernel.fileSystem.remove(fileName))
    	{
    		return 0;
    	}
    	else
    	{
    		return -1;
    	}

    }

    //================================================================================================
    //
    // All code here and below is for internal handling, you can ignore when looking up helper methods
    //
    //================================================================================================
    public enum SysCall
    {
        HALT(0),
        EXIT(1),
        EXEC(2),
        JOIN(3),
        CREATE(4),
        OPEN(5),
        READ(6),
        WRITE(7),
        CLOSE(8),
        UNLINK(9),
        MMAP(10),
        CONNECT(11),
        ACCEPT(12);

        public final int value;
        private static final Map<Integer, SysCall> lookup = new HashMap<Integer, SysCall>();

        private SysCall(int value)
        {
            this.value = value;
        }

        static
        {
            for (SysCall s : values())
            {
                lookup.put(s.value, s);
            }
        }

        public static SysCall lookup(int value)
        {
            return lookup.get(value);
        }
    }

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr>
     * <td>syscall#</td>
     * <td>syscall prototype</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td><tt>void halt();</tt></td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td><tt>void exit(int status);</tt></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td><tt>int exec(char *name, int argc, char **argv);
     * </tt></td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td><tt>int join(int pid, int *status);</tt></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td><tt>int creat(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td><tt>int open(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td><tt>int read(int fd, char *buffer, int size);
     * </tt></td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td><tt>int write(int fd, char *buffer, int size);
     * </tt></td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td><tt>int close(int fd);</tt></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td><tt>int unlink(char *name);</tt></td>
     * </tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0 the first syscall argument.
     * @param a1 the second syscall argument.
     * @param a2 the third syscall argument.
     * @param a3 the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(SysCall syscall, int a0, int a1, int a2, int a3)
    {
        switch (syscall)
        {
            case HALT:
                return handleHalt();
            case EXIT:
                return handleExit(a0);
            case EXEC:
                return handleExec((char) a0, a1, (char) a2, (char) a3);
            case JOIN:
                return handleJoin(a0, a1);
            case CREATE:
                return handleCreate((char) a0);
            case OPEN:
                return handleOpen((char) a0);
            case READ:
                return handleRead(a0, (char) a1, a2);
            case WRITE:
                return handleWrite(a0, (char) a1, a2);
            case CLOSE:
                return handleClose(a0);
            case UNLINK:
                return handleUnlink(a0);
            case MMAP:
            case CONNECT:
            case ACCEPT:
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
     * . The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause)
    {
        Processor processor = Machine.processor();

        switch (cause)
        {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                                           processor.readRegister(Processor.regA0),
                                           processor.readRegister(Processor.regA1),
                                           processor.readRegister(Processor.regA2),
                                           processor.readRegister(Processor.regA3));
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3)
    {
        SysCall call = SysCall.lookup(syscall);
        if (null == call)
        {
            Lib.debug(dbgProcess, "Unknown syscall " + syscall);
            Lib.assertNotReached("Unknown system call!");
            return 0;
        }
        else
        {
            return handleSyscall(call, a0, a1, a2, a3);
        }
    }
}
