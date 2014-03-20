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
    public static Hashtable<Integer,Integer> ProcessToPageTable;//pid^vpn, ppn
    
    
    
    
   /* In order to find unreferenced pages to throw out on page faults, you will need to keep
    * track of all of the pages in the system which are currently in use. You should consider
    * using a core map, an array that maps physical page numbers to the virtual pages that are
    * stored there.
    * */
    public static MemoryPage[] physicalMemoryMap;
    public static TLBController tlbController;
    public static MemoryController memoryController;
    public static SwapPageManager swapFile;
    public static Lock invertedPageTableLock; //only one lock for all pages that prevent other process tries to move page while swapping
    
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() 
	{
		super();
	}

	/**
	 * Initialize this kernel.
     * @param args
	 */
        @Override
	public void initialize(String[] args) 
	{
		super.initialize(args);
		physicalMemoryMap = new MemoryPage[Machine.processor().getNumPhysPages()];
		ProcessToPageTable = new Hashtable<Integer,Integer>();
		tlbController = new TLBController();
                invertedPageTableLock = new Lock();
                memoryController = new MemoryController();
                SwapPageManager.initialize();
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
		SwapPageManager.close();
		printDebug(memoryController.pageReplacementAlgorithm.getAlgorithmName()+
				"Total Page Fault: "+memoryController.pageReplacementAlgorithm.getNumberPageFault());
                
                super.terminate();
	}
        
    public static TranslationEntry getMemoryPageEntryFromPhyMem(int ProcessId, int virtualPageNumber)
    {
        TranslationEntry entry = null;
        
        //Determine PPN from invertedPageTable
        Integer ppn = ProcessToPageTable.get(ProcessId ^ virtualPageNumber);
        
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
    
    
    /**
     * Handle a Page Fault for UserProcess handle missing page in physical
     * memory
     *
     * @param pid
     * @param vpn missing virtual page number
     * @return
     */
    public static TranslationEntry handlePageFault(int pid, int vpn)
    {
    	printDebug(UThread.currentThread().getName() + ", handleTLB miss exception: " + vpn);
    	/*
    	 * Now that pages are being moved to and from memory and disk, you need to ensure that
    	 * one process won't try to move a page while another process is performing some other
    	 * operation on it (e.g., areadVirtualMemory or writeVirtualMemory operation, or loading the
    	 * contents of the page from a disk file). You should not use a separate Lock for every page
    	 * -- this is highly inefficient. ---> used globe lock
    	 */
    	invertedPageTableLock.acquire();
    	
    	//Swap in and also swap out within the same method
    	TranslationEntry missedTranslatedEntry = memoryController.swapIn(pid, vpn);
    	
    	invertedPageTableLock.release();
    	
    	return missedTranslatedEntry;
    }
    
    public static void printDebug(String message)
    {
        Lib.debug(dbgVM, message);
    }
}
