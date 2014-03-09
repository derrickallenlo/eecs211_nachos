package nachos.vm;

import java.util.Hashtable;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel 
{
    
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
        
        public static Hashtable<Integer,Integer> invertedPageTable;
        public static Lock invertedPageTableLock;
        public static MemoryPage[] physicalMemoryMap;
        public static TLBController tlbController;
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() 
        {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) 
        {
		super.initialize(args);
                physicalMemoryMap = new MemoryPage[Machine.processor().getNumPhysPages()];
                invertedPageTable = new Hashtable<Integer,Integer>();
                tlbController = new TLBController();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() 
        {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() 
        {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() 
        {
		super.terminate();
	}
        
        public static TranslationEntry getMemoryPageEntryFromPhyMem(int ProcessId, int virtualPageNumber)
        {
            TranslationEntry entry = null;
            
            //Determine PPN from invertedPageTable
            Integer ppn = invertedPageTable.get(ProcessId ^ virtualPageNumber);
            
            //search physical memory map using hashkey
            if (ppn != null)
            {
                if (physicalMemoryMap[ppn].entry != null || physicalMemoryMap[ppn].entry.valid)
                {
                    //only return entry if valid & not null
                    return physicalMemoryMap[ppn].entry;
                }
            }
            
            return entry;
        }
}
