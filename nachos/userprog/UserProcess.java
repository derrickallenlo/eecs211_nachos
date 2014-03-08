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
    private OpenFile fileDescribtors[];//tracking file descriptors
    private UThread uThread;
    private static final int pageSize = Processor.pageSize;
    private static final int STRINGS_MAX_LENGTH = 256;
    private static final char dbgProcess = 'a';
    private static Map<Integer, UserProcess> allProcess = new HashMap<Integer, UserProcess>();
    private UserProcess parent;
    private final int processId;
    private BinarySemaphore exitStatusAvailable;
    private int exitStatus;
    private static final int EXIT_STATUS_STILL_ALIVE = 999;       // 0-7 status are used
    private static final int EXIT_STATUS_UNHANDLED_EXC = 1000;
    private static final Lock processIdLock = new Lock();
    
    /**
     * Allocate a new process.
     */
    public UserProcess()
    {

        fileDescribtors = new OpenFile[16];//16 concurrent files include stdin and stdout
        fileDescribtors[0] = UserKernel.console.openForReading();//stdin    
        fileDescribtors[1] = UserKernel.console.openForWriting();//stdout    
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i = 0; i < numPhysPages; i++) 
        {
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
        }
        
        this.exitStatus = EXIT_STATUS_STILL_ALIVE;
        
        //Process creation should give unique ID's
        processIdLock.acquire();
        processId = allProcess.size();
        allProcess.put(processId, this);
        processIdLock.release();
        
        exitStatusAvailable = new BinarySemaphore();
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

        uThread = new UThread(this);
        uThread.setName(name).fork();

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
    
    public String readVirtualMemoryString(int vaddr)
    {
        return readVirtualMemoryString(vaddr, STRINGS_MAX_LENGTH);
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
        return virtualMemoryCommandHandler(vaddr, data, offset, length, true);
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
        return virtualMemoryCommandHandler(vaddr, data, offset, length, false);
        /*
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
        */
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
        printDebug( "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null)
        {
            printDebug( "\topen failed");
            return false;
        }

        try
        {
            coff = new Coff(executable);
        }
        catch (EOFException e)
        {
            executable.close();
            printDebug( "\tcoff load failed");
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
                printDebug( "\tfragmented executable");
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
            printDebug( "\targuments too long");
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
        int nextFreePhysicalPage = checkForContiguousBlocks(numPages);
        
        if ((numPages > Machine.processor().getNumPhysPages()) || (nextFreePhysicalPage == -1)) 
        {
            coff.close();
            printDebug( "\tinsufficient physical memory");
            return false;
        }
        
        pageTable = new TranslationEntry[numPages];
                
        //System.out.println("Next Free Physical Page: " + nextFreePhysicalPage);
                
    	for (int i=0; i < numPages; i++)
        {
           int nextFreePage = UserKernel.freePhysicalPages.remove(UserKernel.freePhysicalPages.indexOf(nextFreePhysicalPage + i));
           pageTable[i] = new TranslationEntry(i, nextFreePage, true, false, false, false);
        }
        
        // load sections
        for (int s = 0; s < coff.getNumSections(); s++)
        {
            CoffSection section = coff.getSection(s);

            printDebug( "\tinitializing " + section.getName()
                                  + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++)
            {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, pageTable[vpn].ppn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections()
    {
        UserKernel.freePagesLock.acquire();

        for (int i=0; i<numPages; i++)
        {
            UserKernel.freePhysicalPages.add(pageTable[i].ppn);
        }

        UserKernel.freePagesLock.release();
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
    
    private boolean isOrphan()
    {
        return (null == parent) || (parent.exitStatus != EXIT_STATUS_STILL_ALIVE);
    }
    
    private UThread getThread()
    {
        return uThread;
    }
    
    private int getExitStatus()
    {
        return exitStatus;
    }
    
    /**
     * Halt the Nachos machine by calling Machine.halt(). Only the root process
     * (the first process, executed by UserKernel.run()) should be allowed to
     * execute this syscall. Any other process should ignore the syscall and
     * return immediately.
     */
    private int handleHalt()
    {
    	if (processId != 0)
    	{
    		printDebug("Caught non-root process tries to halt");
    		return 0;
    	}
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
    		printDebug("process call exist id: " + processId);
    		
    		// close all the open files threads 
    		for (OpenFile file : fileDescribtors) 
                {
                    if (null != file) 
                    {
                        file.close();
                    }
                }			

    		exitStatus = status;    //set exit status

    		// Free virtual memory
    		unloadSections();
                
                exitStatusAvailable.signal();

    		// last process call the machine to halt
    		if (processId == 0) 
                {
    			Kernel.kernel.terminate(); //root exiting
    		} 
                else 
                {
    			KThread.finish();
    		}			   						    	   		      

        return exitStatus; //this never happens...
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
    private int handleExec(int name, int argc, int argv)
    {
        boolean exeLoaded, memoryAccessSuccess;
        String exeFile = readVirtualMemoryString(name);
        if (null == exeFile)
        {
            printDebug( "Error in opening file id:" + name);
            return -1;
        }
        else if (!exeFile.endsWith(".coff"))
        {
            printDebug( "File found does not end in .coff");
            return -1;
        }
        else if (argc < 0)
        {
            printDebug( "Cannot have negative arguements");
            return -1;
        }
        
        //Part 2 build the Args                 
        String[] args = new String[argc];
        IntegerBufferMap data = new IntegerBufferMap(argc);
        memoryAccessSuccess = data.readMemoryIntoData(argv);
        if (!memoryAccessSuccess)
        {
            printDebug( "Failed to read integer contents from memory");
            return -1;
        }
        
        for(int i=0; i < data.size(); i++)
        {
            int value = data.getIntLE(i);
            args[i] = readVirtualMemoryString(value);
            if (null == args[i])
            {
                printDebug( "Error in reading String from memory:" + value);
                return -1;
            }
        }
        
        //Part 3 execution
        UserProcess child = newUserProcess();
        
        saveState();
        
        exeLoaded = child.execute(exeFile, args);
        if (exeLoaded) //program executed, child was success
        {
            child.parent = this;
            return child.processId;
        } 
        else
        {
            printDebug( "Failed to load executable, no child created");
            return -1;
        }
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
        UserProcess child = allProcess.get(pid);
        if (null == child)
        {
            printDebug("Child does not exist for pid: " + pid);
            return -1;
        }
        else if (this != child.parent)
        {
            printDebug("Pid " +pid+" is not this process' child");
            return -1;
        }
        
        allProcess.put(pid, null);  //remove reference to child (can't reuse id)
        child.parent = null;        //dereference parent in case parent deleted
        
        child.exitStatusAvailable.waitFor();
        int childExitStatus = child.getExitStatus();
        IntegerBufferMap data = new IntegerBufferMap(1);
        data.setIntLE(0, childExitStatus);
        boolean successfulStatusWrite = data.writeToMemoryFromData(status);
        if (!successfulStatusWrite)
        {
            printDebug("Failed to write child exist status to memory:" + childExitStatus);
            return 0;
        }

        //Program executed syscall succesfully
        if (childExitStatus != EXIT_STATUS_UNHANDLED_EXC          //some kernel exception
            && childExitStatus != EXIT_STATUS_STILL_ALIVE)        //did not call exit()
        {
            return 1;
        }
        else
        {
            printDebug("Child exited with status: " + childExitStatus);
            return 0;
        }

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
    private int handleCreate(int name)
    {
    	if (name < 0)
    	{
    		Lib.debug(dbgProcess, "\t Negative virtual address name detected");
    		return -1;
    	}
    	
    	//User.Process.readVirtualMemory used transfer between the user process and kernel
    	//with system calls is 256 bytes
    	String fileName = readVirtualMemoryString (name);
    	if (fileName == null)
    	{
    		Lib.debug(dbgProcess, "\t Null file name detected - handleCreate");
    		return -1;
    	}
    	
    	//found FileDecriptorIndex of supporting max 16 concurrent files per user program
    	int firstAvailableIndex = -1;
    	for (int fdIndex = 0; fdIndex < 16; fdIndex++)
    	{
    		//scan all 16 spaces - not creating if it does exist
    		if ((fileDescribtors[fdIndex] != null) && (fileDescribtors[fdIndex].getName().equals(fileName)) )
    		{
    			printDebug("\t The same file name detected - handleCreate");
    			return -1;
    		}
    		
    		if ((fileDescribtors[fdIndex] == null) && (firstAvailableIndex < 0))//creating it if it does not exist
    		{
    			firstAvailableIndex = fdIndex;
    		}
    	}
    	
    	if (firstAvailableIndex > 0)
    	{
    		printDebug("\tCreating File...");
			fileDescribtors[firstAvailableIndex] = ThreadedKernel.fileSystem.open(fileName, true);//making new one set to true
			return firstAvailableIndex;
    	}
		
    	printDebug("\tFile Descriptor has reached the maxium - 16 concurrent files - handleCreate");
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
    private int handleOpen(int name)
    {
    	if (name < 0)
    	{
    		printDebug("\t Negative virtual address name detected - handleOpen");
    		return -1;
    	}
    	
    	//User.Process.readVirtualMemory used transfer between the user process and kernel
    	//with system calls is 256 bytes
    	String fileName = readVirtualMemoryString (name);
    	if (fileName == null)
    	{
    		printDebug("\t Null file name detected - handleOpen");
    		return -1;
    	}
    	
    	//find it from array of file decribtors 
    	for (int fdIndex = 0; fdIndex < 16; fdIndex++)
    	{
    		if (fileDescribtors[fdIndex] != null && fileDescribtors[fdIndex].getName().equals(fileName))
    		{
    			printDebug("\t Opening File...");
    			ThreadedKernel.fileSystem.open(fileName, false);//not making the new file set to false
    			return fdIndex;
    		}
    	}
    	
    	//if not in current file descibtors, find file and load to fd
    	int firstAvailableIndex = -1;
    	for (int fdIndex = 0; fdIndex < 16; fdIndex++)
    	{
    		//scan all 16 spaces - not creating if it does exist
    		if ((fileDescribtors[fdIndex] != null) && (fileDescribtors[fdIndex].getName().equals(fileName)) )
    		{
    			printDebug("\t The same file name detected - handleOpen");
    			return -1;
    		}
    		
    		if ((fileDescribtors[fdIndex] == null) && (firstAvailableIndex < 0))//creating it if it does not exist
    		{
    			firstAvailableIndex = fdIndex;
    		}
    	}
    	
    	if (firstAvailableIndex > 0)
    	{
			fileDescribtors[firstAvailableIndex] = ThreadedKernel.fileSystem.open(fileName, false);//making new one set to true
			if (fileDescribtors[firstAvailableIndex] !=null)
			{
				printDebug("\t Opening File...");
				return firstAvailableIndex;
			}
			else
			{
				printDebug("\t Can't open the file or folder is not supported - handleOpen");
				return -1;
			}
    	}
    	printDebug("\tFile Descriptor has reached the maxium - 16 concurrent files - handleOpen");
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
    private int handleRead(int fd, int buffer, int size)
    {
    	int numberBytes = 0;//number of bytes read 
    	
    	//fileDescriptor is invalid
    	if ((fd < 0) || (fd > 15))
    	{
    		printDebug("\t Invalid FileDecriptor index - handleRead");
    		return -1;
    	}
    	
    	//count bytes - size - is invalid
    	if (size < 0)
    	{
    		printDebug("\t Invalid size of reading count bytes - handleRead");
    		return -1;
    	}
    	else if (size == 0)
    	{
    		printDebug("\t 0 reading count bytes detected - handleRead");
    		return 0;
    	}
    	
    	if (fileDescribtors[fd] == null)
    	{
    		printDebug("\t Invalid file decribtor - handleRead");
    		return -1;
    	}
    	
    	OpenFile file = fileDescribtors[fd];
    	if (file == null)
    	{
    		printDebug("\t FileDecriptor is not found in the opened files - handleRead");
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
    		printDebug("\t read file error - handleRead");
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
    		printDebug("\t write to virtual memory error - handleRead");
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
    private int handleWrite(int fd, int buffer, int size)//buffer is virtual address
    {
    	int numberBytes = 0;//number of bytes read 
    	
    	//fileDescriptor is invalid
    	if ((fd < 0) || (fd > 15))
    	{
    		printDebug("\t Invalid FileDecriptor index - handleWrite");
    		return -1;
    	}
    	
    	//count bytes - size - is invalid
    	if (size < 0)
    	{
    		printDebug("\t Invalid size of reading count bytes - handleWrite");
    		return -1;
    	}
    	else if (size == 0)
    	{
    		printDebug("\t 0 reading count bytes detected - handleWrite");
    		return 0;
    	}
    	
    	OpenFile file = fileDescribtors[fd];
    	if (file == null)
    	{
    		printDebug("\t FileDecriptor is not found in the opened files - handleWrite");
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
    		printDebug("\t read data from virtual memory error - handleWrite");
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
    		printDebug("\t write file error - handleWrite");
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
    		printDebug("\t Invalid FileDecriptor index - handleClose");
    		return -1;
    	}
    	
    	if (fileDescribtors[fd] == null)
    	{
    		printDebug("\t NULL FileDecriptor found - handleClose");
    		return -1;
    	}
    	
    	fileDescribtors[fd].close();
    	fileDescribtors[fd] = null;
    	printDebug("\t file closed succesfully - handleClose");
    	
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
        	printDebug("\t Invalid address - handleUnlink");
    		return -1;
        }
        
        String fileName = readVirtualMemoryString(addr);
        
        if (fileName == null)
        {
        	printDebug("\t Invalid File Name - handleUnlink");
    		return -1;
        }
        
        //make file Describtors available to the process if any file open match
        for (int i = 0; i < 16; i++)
        {
        	if (fileDescribtors[i] != null && fileDescribtors[i].getName().equals(fileName))
        	{
        		fileDescribtors[i].close();//release any system source
        		fileDescribtors[i] = null;
        	}
        }
        
    	if(ThreadedKernel.fileSystem.remove(fileName))
    	{
    		printDebug("\t File Name unlink successfuly - handleUnlink");
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
                return handleExec( a0, a1,  a2);
            case JOIN:
                return handleJoin(a0, a1);
            case CREATE:
                return handleCreate( a0);
            case OPEN:
                return handleOpen( a0);
            case READ:
                return handleRead(a0,  a1, a2);
            case WRITE:
                return handleWrite(a0,  a1, a2);
            case CLOSE:
                return handleClose(a0);
            case UNLINK:
                return handleUnlink(a0);
            case MMAP:
            case CONNECT:
            case ACCEPT:
            default:
                printDebug( "Unknown syscall " + syscall);
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
                printDebug( "Unexpected exception: " + Processor.exceptionNames[cause]);
                exitStatus = EXIT_STATUS_UNHANDLED_EXC;
                break;
        }
    }

    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3)
    {
        SysCall call = SysCall.lookup(syscall);
        if (null == call)
        {
            printDebug( "Unknown syscall " + syscall);
            Lib.assertNotReached("Unknown system call!");
            return 0;
        }
        else
        {
            return handleSyscall(call, a0, a1, a2, a3);
        }
    }
    
    private static void printDebug(String message)
    {
        Lib.debug(dbgProcess, message);
    }
    
    public class IntegerBufferMap 
    {
        private final byte[] data;
        private static final int BYTES_IN_INTEGER = 4;

        public IntegerBufferMap(byte[] data)
        {
            this.data = data;
        }

        public IntegerBufferMap(int size) 
        {
            this.data = new byte[size * BYTES_IN_INTEGER];
        }
        
        public boolean readMemoryIntoData(int addr)
        {
            return readVirtualMemory(addr, data) == (data.length);
        }
        
        public boolean writeToMemoryFromData(int addr)
        {
            return writeVirtualMemory(addr, data) == (data.length);
        }
        
        public int getIntLE(int intOffset)
        {
            byte[] dest = new byte[BYTES_IN_INTEGER];
            int integerOffset = intOffset * BYTES_IN_INTEGER;
            System.arraycopy(data, integerOffset, dest, 0, BYTES_IN_INTEGER);
            return Lib.bytesToInt(data, integerOffset);
        }
        
        public void setIntLE(int intOffset, int value)
        {
            byte[] src = Lib.bytesFromInt(value);
            System.arraycopy(src, 0, data, intOffset * BYTES_IN_INTEGER , BYTES_IN_INTEGER);
        }
        
        
        public byte[] getData()
        {
            return data;
        }
        
        public int size()
        {
            return data.length / BYTES_IN_INTEGER;
        }
        
    }
    
    public class BinarySemaphore extends Semaphore 
    {
        public BinarySemaphore() 
        {
            super(0);
        }

        public void signal()
        {
            V();
        }
        
        public void waitFor()
        {
            P();
        }
    }
    
    /*
    * Function to determine best location in Physical memory to 
    * to create a contigious block of Virtual Memory
    * 
    * @param numberOfPagesNeeded number of Physical Pages needed by Process
    */
    private int checkForContiguousBlocks(int numberOfPagesNeeded)
    {
        int startIndex = 0;
        int counter = 0;
        int lastPage = 0;
        boolean blockNotFound = true;
        UserKernel.freePagesLock.acquire();

        startIndex = UserKernel.freePhysicalPages.peekFirst();

        //System.out.println("# of Pages Needed: " + numberOfPagesNeeded);

        for (Integer freePage : UserKernel.freePhysicalPages)
        {
            if (freePage == startIndex)
            {
                //init counter
                counter = 1;
                lastPage = freePage;
                //System.out.println("\n Current Page: " + freePage + " Counter: " + counter + " lastPage: " + lastPage + " startIndex: " + startIndex);
            }
            else if (counter == numberOfPagesNeeded)
            {
                blockNotFound = false;
                break;
            }
            else if (freePage == (lastPage + 1))
            {
                counter++;
                lastPage = freePage;
                //System.out.println("\n Current Page: " + freePage + " Counter: " + counter + " lastPage: " + lastPage + " startIndex: " + startIndex);
            }

            else
            {
                //Hole in Physical Memory Detected! Reset startIndex
                startIndex = freePage;
                lastPage = freePage;
                counter = 1;
                //System.out.println("Hole Found! Restarting search..");
                //System.out.println("\n Current Page: " + freePage + " Counter: " + counter + " lastPage: " + lastPage + " startIndex: " + startIndex);
            }
        }

        UserKernel.freePagesLock.release();

        if (!blockNotFound)
        {
            //System.out.println("Allocation found!, Returning index; " + startIndex);
            return startIndex;
        }
        else
        {
            //THROW ERROR IF INDEX IS STILL NOT FOUND!
            return -1;
        }
    }
    
    private int virtualMemoryCommandHandler(int vaddr, byte[] data, int offset, int length, boolean readCommand)
    {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        
        int firstPageToXfer = Machine.processor().pageFromAddress(vaddr);
	int lastPageToXfer = Machine.processor().pageFromAddress(vaddr+length);
        int amount = 0;
        
        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
        {
            return 0;
        }
        
        for(int i = firstPageToXfer; i <= lastPageToXfer; i++)
        {
            int startVAddr = Machine.processor().makeAddress(i, 0);
            int endVAddr = startVAddr + (pageSize - 1);
            int pageOffsetStart;
            int pageOffsetEnd;
            
            if(pageTable[i].valid != true)
            {
                break;
            }
            if(!readCommand && pageTable[i].readOnly)
            {
                //Do not initiate write command if page is Read Only
                break;
            }
            
            
            //copies entire page
            if (vaddr+length >= endVAddr)
            {
                if (vaddr <= startVAddr)
                {
                   pageOffsetStart = 0;
                   pageOffsetEnd = pageSize - 1; 
                }
                else
                {
                   pageOffsetStart = vaddr - startVAddr;
                   pageOffsetEnd = pageSize - 1;    
                }
            }
            //copy begin of page to not quite the end
            else if (vaddr <= startVAddr && vaddr+length < endVAddr)
            {
                    pageOffsetStart = 0;
                    pageOffsetEnd = (vaddr + length) - startVAddr;
            }
            //copy partial portion of page where offset is not aligned to beginning or end
            else 
            {
                    pageOffsetStart = vaddr - startVAddr;
                    pageOffsetEnd = (vaddr + length) - startVAddr;
            }
            
            if (readCommand)
            {
                System.arraycopy(memory, Machine.processor().makeAddress(pageTable[i].ppn, pageOffsetStart), data, offset+amount, pageOffsetEnd-pageOffsetStart); 
            }
            else
            {
                System.arraycopy(data, offset+amount, memory, Machine.processor().makeAddress(pageTable[i].ppn, pageOffsetStart), pageOffsetEnd-pageOffsetStart);
            }
      
            amount += (pageOffsetEnd-pageOffsetStart);
            
            pageTable[i].used = true;
            
            if(!readCommand)
            {
                pageTable[i].dirty = true;
            }
        }
        
        return amount;
    }
}
