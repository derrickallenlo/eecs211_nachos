package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess 
{
        private LazyCoffLoader loader;
	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
	private static final char dbgVM = 'v';
        
	/**
	 * Allocate a new process.
	 */
	public VMProcess() 
    {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() 
        {
		VMKernel.tlbController.clear();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() 
        {
		//super.restoreState();         //delete this?
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() 
        {
            loader = new LazyCoffLoader(coff);
            
	//	return super.loadSections();
            return true;   
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() 
        {
            
		super.unloadSections();
                //TODO BY DERRICk
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
            switch (cause)
            {
                case Processor.exceptionTLBMiss:
                    handleTLBMiss();
                    break;
                default:
                    super.handleException(cause);
                    break;
            }
        }
        
        /**
	 * Handle a TLB Miss exception.
	 * Page translation was not found in TLB, translate Page # from inverted Page Table
	 * 
	 */
        private void handleTLBMiss()
        {
            int missedVirtualPage = Machine.processor().pageFromAddress(Machine.processor().readRegister(Processor.regBadVAddr));
            
            printDebug( "TLB Miss! On Virtual Page " + missedVirtualPage);
            
            //Retrieve Physical translation and resume process?
            TranslationEntry translatedEntry = VMKernel.getMemoryPageEntryFromPhyMem(super.getProcessID() , missedVirtualPage);
            
            if (translatedEntry == null)
            {
                //PAGEFAULT! handle page fault accordingly
            	//Kernel handle the page fault
            	//1. Keep tracking current page is used to find unreferenced pages to throw out on page faults
            	translatedEntry = VMKernel.handlePageFault(super.getProcessID(), missedVirtualPage);
            }
          
            //Write out translation to TLB
            if (translatedEntry != null)
            {
                VMKernel.tlbController.addEntry(translatedEntry);
            }
            else
            {
                // TODO what happens here?
            }
        }
}
