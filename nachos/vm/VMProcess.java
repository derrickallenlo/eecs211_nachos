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
    private LoaderForCoff loader;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    private static final boolean PRINT_UNLOAD_MESSAGES = false;

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
    @Override
    public void saveState()
    {
        VMKernel.printDebug("Clearing the TLB!");
        TLBController.invalidateAllTlbEntry();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    @Override
    public void restoreState()
    {
        //super.restoreState();         //using TLB, so can't
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return <tt>true</tt> if successful.
     */
    @Override
    protected boolean loadSections()
    {
        loader = new LoaderForCoff(coff);

        //	return super.loadSections();
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    @Override
    protected void unloadSections()
    {
        coff.close();
        loader = null;

        TLBController.invalidateAllTlbEntry();
        if (PRINT_UNLOAD_MESSAGES)
        {
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            VMKernel.printDebug("      ****FILE EXITED!!");

            VMKernel.printDebug("      ****Printing the inverted page table");
            VMKernel.printDebug(InvertedPageTable.getString());
            VMKernel.printDebug("      ****Printing the physical page table");
            for (int tempPpn = 0; tempPpn < VMKernel.physicalDiskMap.length; tempPpn++)
            {
                if (VMKernel.physicalDiskMap[tempPpn] == null)
                {
                    continue;
                }

                VMKernel.printDebug("      **vpn: " + VMKernel.physicalDiskMap[tempPpn].getVirtualPageNumber() + ", ppn: " + tempPpn);
                VMKernel.printDebug(VMKernel.physicalDiskMap[tempPpn].entry);
            }//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        }

        //Clear out Inverted Page table
        for (int vpn = 0; vpn < numPages; vpn++)
        {
            Integer ppn = InvertedPageTable.remove(super.getProcessID(), vpn);

            if (ppn != null)
            {
                VMKernel.physicalDiskMap[ppn].entry.valid = false;
                
                VMKernel.printDebug("Removing for reuse a ppn: " + ppn);
                SwapPageController.removePage(ppn);
            }

            SwapPageManager.removeSwapPage(super.getProcessID(), vpn);
        }

        if (PRINT_UNLOAD_MESSAGES)
        {
            //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            VMKernel.printDebug("      ****IT WAS BEEN CLEARED!!");

            VMKernel.printDebug("      ****Printing the inverted page table");
            VMKernel.printDebug(InvertedPageTable.getString());
            VMKernel.printDebug("      ****Printing the physical page table");
            for (int tempPpn = 0; tempPpn < VMKernel.physicalDiskMap.length; tempPpn++)
            {
                if (VMKernel.physicalDiskMap[tempPpn] == null)
                {
                    continue;
                }

                VMKernel.printDebug("      **vpn: " + VMKernel.physicalDiskMap[tempPpn].getVirtualPageNumber() + ", ppn: " + tempPpn);
                VMKernel.printDebug(VMKernel.physicalDiskMap[tempPpn].entry);
            }//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        }

    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
     * . The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    @Override
    public void handleException(int cause)
    {
        switch (cause)
        {
            case Processor.exceptionTLBMiss:
                handleTLBMiss(Machine.processor().pageFromAddress(Machine.processor().readRegister(Processor.regBadVAddr)));
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

    /**
     * Handle a TLB Miss exception. Page translation was not found in TLB,
     * translate Page # from inverted Page Table
     *
     */
    private void handleTLBMiss(int missedVirtualPage)
    {

        VMKernel.printDebug("=================================");
        VMKernel.printDebug("TLB Miss! Process: " + super.getProcessID() + " On Virtual Page " + missedVirtualPage);
            //TLBController.printTLBTable();

        //Retrieve Physical translation and resume process?
        TranslationEntry translatedEntry = VMKernel.searchInvertedPageTable(super.getProcessID(), missedVirtualPage);

        if (translatedEntry == null)
        {
            //PAGEFAULT! handle page fault accordingly
            //Kernel handle the page fault
            //1. Keep tracking current page is used to find unreferenced pages to throw out on page faults
            translatedEntry = VMKernel.handlePageFault(super.getProcessID(), missedVirtualPage, loader);
        }

        //Write out translation to TLB
        if (translatedEntry != null)
        {
            TLBController.addTlb(translatedEntry);
        }
        else
        {
            //ERROR! Could not find page
            handleException(SysCall.EXIT.value);
        }

        //TLBController.printTLBTable();
    }

    @Override
    protected int virtualMemoryCommandHandler(int vaddr, byte[] data, int offset, int length, boolean readCommand)
    {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        int firstPageToXfer = Processor.pageFromAddress(vaddr);
        int lastPageToXfer = Processor.pageFromAddress(vaddr + length);
        int amount = 0;

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
        {
            return 0;
        }

        for (int i = firstPageToXfer; i <= lastPageToXfer; i++)
        {
            int startVAddr = Processor.makeAddress(i, 0);
            int endVAddr = startVAddr + (pageSize - 1);
            int pageOffsetStart;
            int pageOffsetEnd;

            TranslationEntry te = TLBController.useTlbEntry(i);
            if (te == null)
            {
                handleTLBMiss(i);
                te = TLBController.useTlbEntry(i);
            }

            if (te.valid != true)
            {
                break;
            }
            if (!readCommand && te.readOnly)
            {
                //Do not initiate write command if page is Read Only
                break;
            }

            //copies entire page
            if (vaddr + length >= endVAddr)
            {
                if (vaddr <= startVAddr)
                {
                    pageOffsetStart = 0;
                }
                else
                {
                    pageOffsetStart = vaddr - startVAddr;
                }

                pageOffsetEnd = pageSize - 1;
            }
            //copy begin of page to not quite the end
            else if (vaddr <= startVAddr && vaddr + length < endVAddr)
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
                System.arraycopy(memory, Machine.processor().makeAddress(te.ppn, pageOffsetStart), data, offset + amount, pageOffsetEnd - pageOffsetStart);
            }
            else
            {
                System.arraycopy(data, offset + amount, memory, Machine.processor().makeAddress(te.ppn, pageOffsetStart), pageOffsetEnd - pageOffsetStart);
            }

            amount += (pageOffsetEnd - pageOffsetStart);

            te.used = true;

            if (!readCommand)
            {
                te.dirty = true;
            }
        }

        return amount;
    }
}
