package nachos.vm;

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

    /* In order to find unreferenced pages to throw out on page faults, you will need to keep
     * track of all of the pages in the system which are currently in use. You should consider
     * using a core map, an array that maps physical page numbers to the virtual pages that are
     * stored there.
     * */
    public static MemoryPage[] physicalDiskMap;
    public static SwapPageManager swapFile;
    private static Lock pageFaultLock; //only one lock for all pages that prevent other process tries to move page while swapping
    //public static boolean DEBUG_ON = false;

    /**
     * Allocate a new VM kernel.
     */
    public VMKernel()
    {
        super();
    }

    /**
     * Initialize this kernel.
     *
     * @param args
     */
    @Override
    public void initialize(String[] args)
    {
        super.initialize(args);
        physicalDiskMap = new MemoryPage[Machine.processor().getNumPhysPages()];
        InvertedPageTable.initialize();
        pageFaultLock = new Lock();
        SwapPageController.initialize();
        SwapPageManager.initialize();
    }

    /**
     * Test this kernel.
     */
    @Override
    public void selfTest()
    {
//		super.selfTest();
    }

    /**
     * Start running user programs.
     */
    @Override
    public void run()
    {
        super.run();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    @Override
    public void terminate()
    {
        SwapPageManager.closeTestFile();
        //	printDebug(memoryController.pageReplacementAlgorithm.getAlgorithmName()+
        //			"Total Page Fault: "+memoryController.pageReplacementAlgorithm.getNumberPageFault());

        super.terminate();
    }

    public static TranslationEntry searchInvertedPageTable(int processId, int virtualPageNumber)
    {
        //Determine PPN from invertedPageTable
        Integer ppn = InvertedPageTable.get(processId, virtualPageNumber);
        printDebug(String.format(" Searching Inverted Page Table: Process: %d vpn: %d ppn: %d", processId, virtualPageNumber, ppn));

        //search physical memory map using hashkey
        if (ppn != null)
        {
            TranslationEntry temp = physicalDiskMap[ppn].entry;
            if (temp == null)
            {
                printDebug("    this ppn has no translation entry, returning null");
                return null;
            }
            else if (temp.valid == false)
            {
                //only return entry if valid & not null
                printDebug("    this ppn is invalid, so it's okay to overwrite it");
                return null;
            }
            else
            {
                printDebug("    this ppn is a okay, returning translation entry");
                return temp;
            }
        }
        else
        {
            printDebug("    this ppn is not applocated, returning null");
            return null;
        }
    }

    /**
     * Handle a Page Fault for UserProcess handle missing page in physical
     * memory
     *
     * @param pid
     * @param vpn missing virtual page number
     * @param loader
     * @return
     */
    public static TranslationEntry handlePageFault(int pid, int vpn, LoaderForCoff loader)
    {
        printDebug(UThread.currentThread().getName() + ": handleTLB miss exception, page fault: " + vpn);
        /*
         * Now that pages are being moved to and from memory and disk, you need to ensure that
         * one process won't try to move a page while another process is performing some other
         * operation on it (e.g., areadVirtualMemory or writeVirtualMemory operation, or loading the
         * contents of the page from a disk file). You should not use a separate Lock for every page
         * -- this is highly inefficient. ---> used globe lock
         */
        pageFaultLock.acquire();

        //Swap in and also swap out within the same method
        TLBController.flushAllTlb();
        TranslationEntry missedTranslatedEntry = SwapPageController.swapIn(pid, vpn, loader);

        pageFaultLock.release();

        return missedTranslatedEntry;
    }

    public static void printDebug(String message)
    {
        Lib.debug(dbgVM, message);
    }

    public static void printDebug(TranslationEntry currentTLBEntry)

    {
        printDebug("      **  VPN: " + currentTLBEntry.vpn + "PPN: "
                   + currentTLBEntry.ppn + " IsValid: " + currentTLBEntry.valid
                   + " Used: " + currentTLBEntry.used + " ReadOnly: "
                   + currentTLBEntry.readOnly + " Dirty: " + currentTLBEntry.dirty);
    }
}
