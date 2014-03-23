package nachos.vm;

import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

/**
 * A <tt>MemroyController</tt>
 *
 * that handle physical memory Control such as: page swapping between physical
 * memory and disk memory remove/move pages if page has moved from/to disk
 * memory but also keep track TLB
 */
public class SwapPageController
{
    //getting ipt if need swap in to memory also update the ipt
    private static ReplacementAlgorithm pageReplacementAlgorithm;
    private static final boolean printAllTables = false;
    private static final boolean printSwapFlag = false;

    private SwapPageController()
    {

    }

    public static void initialize()
    {
        //initilize usedFrame
        pageReplacementAlgorithm = new SecondChanceReplacement();
    }

    /**
     * swap out physical page from physical memory
     *
     * @param ppn physical page number has to swap to disk memory
     */
    private static void swapOut(int ppn)
    {
        VMKernel.printDebug("     Checking if swapping out from table, ppn: " + ppn);
        MemoryPage swapOutPage = VMKernel.physicalDiskMap[ppn];

        //make sure it's in the memory
        //if it's not in the memory, we don't need swap out
        if (swapOutPage != null && swapOutPage.entry.valid)
        {
            int vpn = swapOutPage.entry.vpn;
            VMKernel.printDebug("     ->Requires remove from table, vpn: " + vpn);
            swapOutPage.entry.valid = false;
            InvertedPageTable.remove(swapOutPage.getOwningProcessId(), vpn);
            TLBController.invalidateTlb(swapOutPage);

            //if modified, update value at disk (ie. write() )
            //otherwise should not write any pages to the swap file
            //Your page-replacement policy should not write any pages to the swap file...
            if (swapOutPage.entry.dirty)
            {
                //update disk value with this entry
                SwapPageManager.SwapPage swapPage = SwapPageManager.createSwapPage(swapOutPage);
                boolean success = SwapPageManager.accessSwapFile(swapPage, ppn, true);
                if (!success)
                {
      
                    //write error and kill proceess
                    VMKernel.printDebug("Write error, unable to swap out!");
                    throw new IllegalArgumentException("write error!");
                }

            }

            /*
             swapOutPage.entry.valid = false;
             VMKernel.ProcessToPageTable.remove(ppn);
             */
        }
        else
        {
            VMKernel.printDebug("     ->No vpp or swap page for ppn");
        }
    }

    /**
     * find a page (page replacement algorithm) that has to swap out from
     * physical memory and the missing page can swap in to physical memory
     *
     * @param pid associated pid in inverted page table that vpn has not yet
     * brought to memory
     * @param vpn missing virtual page number that has to swap in to physical
     * memory
     * @param loader
     * @return
     */
    public static TranslationEntry swapIn(int pid, int vpn, LoaderForCoff loader)
    {
        VMKernel.printDebug("Beginning swap in sequence!!!");
        TranslationEntry entry;
        int ppn = pageReplacementAlgorithm.findSwappedPage();
        try
        {
        swapOut(ppn);//if only if it's already in the memory
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
        //now perform Swap In
        
        SwapPageManager.SwapPage swapPage = SwapPageManager.getSwapPage(pid, vpn);
        // if the swapIn Page is on the disk
        if (swapPage != null)
        {
            entry = swapPage.getDiskPage().entry;
            if (printSwapFlag)
            {
                VMKernel.printDebug("    Loading a swap entry-> VPN: " + entry.vpn + "PPN: "
                                    + entry.ppn + " IsValid: " + entry.valid
                                    + " Used: " + entry.used + " ReadOnly: "
                                    + entry.readOnly + " Dirty: " + entry.dirty);
            }
            entry.valid = true;
            entry.used = false;
            entry.dirty = false;
            if (printSwapFlag)
            {
                VMKernel.printDebug("    Rewrite entry-> VPN: " + entry.vpn + "PPN: "
                                    + entry.ppn + " IsValid: " + entry.valid
                                    + " Used: " + entry.used + " ReadOnly: "
                                    + entry.readOnly + " Dirty: " + entry.dirty);
            }
            boolean success = SwapPageManager.accessSwapFile(swapPage,ppn, false);
            if (!success)
            {
                // read error and kill proceess
                VMKernel.printDebug("Read error, unable to swap in file!");
                return null;
            }
        }
        else
        {
            entry = loader.loadData(pid, vpn, ppn);
        }

        //found  a page by now, map virtual to physical
        InvertedPageTable.put(pid, vpn, ppn);//update ipt
        MemoryPage newPage = new MemoryPage(pid, vpn, entry);
        VMKernel.physicalDiskMap[ppn] = newPage;//update Core Map of tracking all ppn

        //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        VMKernel.printDebug("      ****END OF SWAPPING!");
        if (printAllTables)
        {

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
        return entry;
    }

    public static void removePage(int ppn)
    {
        VMKernel.printDebug("      Removing ppn page from unload: " + ppn);
        pageReplacementAlgorithm.removePage(ppn);
    }

}
