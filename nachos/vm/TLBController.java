package nachos.vm;

import java.util.Stack;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

/**
 *
 * @author derrick
 */
public class TLBController
{
    private static final boolean DBG = false;
    private static final Stack<Integer> LRU = new Stack<Integer>();

    private TLBController()
    {

    }

    public static void addTlb(TranslationEntry entry)
    {
        //Search for invalidEntries and replace first one that is found
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            if (false == Machine.processor().readTLBEntry(i).valid)
            {
                printDebug(String.format("    Writing to TLB Entry: %d vpn: %d, ppn: %d", i, entry.vpn, entry.ppn));
                Machine.processor().writeTLBEntry(i, entry);
                //flush(i);
                LRU.remove((Integer) i);
                LRU.push(i);
                return;
            }
        }

        Lib.assertTrue(LRU.size() <= Machine.processor().getTLBSize());

        //If table is full..
        int victim = LRU.firstElement();
        LRU.remove((Integer) victim);
        LRU.push(victim);
        printDebug(String.format("    No free TLB reuse random TLB Entry: %d vpn: %d, ppn: %d", victim, entry.vpn, entry.ppn));
        flushTlbEntry(victim);
        Machine.processor().writeTLBEntry(victim, entry);

    }

    public static TranslationEntry useTlbEntry(int vpn)
    {
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            TranslationEntry entry = Machine.processor().readTLBEntry(i);
            if (entry.valid && entry.vpn == vpn)
            {
                //If entry in memory is valid, set to used
                entry.used = true;
                Machine.processor().writeTLBEntry(i, entry);
                return entry;
            }
        }
        return null;
    }

    //See if MemoryPage is in TLB and mark as as Invalid if not already
    public static void invalidateTlb(MemoryPage pageToInvalidate)
    {
        printDebug("Invalidating entry for  vpn:" + pageToInvalidate.getVirtualPageNumber());
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            if (Machine.processor().readTLBEntry(i).ppn == pageToInvalidate.entry.ppn)
            {
                TranslationEntry temp = pageToInvalidate.entry;
                printDebug("Invalidating entry for  vpn:" + temp.vpn + ", ppn: " + temp.ppn);
                temp.valid = false;
                Machine.processor().writeTLBEntry(i, pageToInvalidate.entry);
            }
        }
    }

    public static void printTLBTable()
    {
        printDebug("===========TLB Table===========");
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            TranslationEntry currentTLBEntry = Machine.processor().readTLBEntry(i);
            printDebug("Index: " + i + " VPN: " + currentTLBEntry.vpn + "PPN: "
                       + currentTLBEntry.ppn + " IsValid: " + currentTLBEntry.valid
                       + " Used: " + currentTLBEntry.used + " ReadOnly: "
                       + currentTLBEntry.readOnly + " Dirty: " + currentTLBEntry.dirty);
        }
    }

    public static void flushAllTlb()
    {
        //Write all TLB entries back to the Disk Page Table
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            flushTlbEntry(i);
        }
    }

    public static void flushTlbEntry(int i)
    {
        flushTlbEntry(Machine.processor().readTLBEntry(i));
    }

    public static void flushTlbEntry(TranslationEntry currentEntry)
    {
        //Only flush valid entries to disk, otherwise it should just be discarded from memory
        if (currentEntry.valid)
        {
            printDebug("    Flushing to ppn map:");
            printDebug(currentEntry);
            VMKernel.physicalDiskMap[currentEntry.ppn].setTranslationEntry(currentEntry);
        }
    }

    //clear all TLB entries by invalidating all entries
    public static void invalidateAllTlbEntry()
    {
        printDebug("Wiping Clear the TLB!");

        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            TranslationEntry entry = Machine.processor().readTLBEntry(i);
            //If entry is still valid when clearing all, then flush it to disk
            if (entry.valid)
            {
                flushTlbEntry(i);   //Flush a new map into physical

                printDebug("    Setting to invalid entry for vpn: " + entry.vpn + ", ppn: " + entry.ppn);
                entry.valid = false;    //then set to invalid so other process can use it

                Machine.processor().writeTLBEntry(i, entry);
            }
        }

        printDebug("    ****Clearing done, show you the core map");
        for (int tempPpn = 0; tempPpn < VMKernel.physicalDiskMap.length; tempPpn++)
        {
            if (VMKernel.physicalDiskMap[tempPpn] == null)
            {
                continue;
            }

            printDebug("      **vpn: " + VMKernel.physicalDiskMap[tempPpn].getVirtualPageNumber() + ", ppn: " + tempPpn);
            printDebug(VMKernel.physicalDiskMap[tempPpn].entry);
        }//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    }

    private static void printDebug(String message)
    {
        if (DBG)
        {
            VMKernel.printDebug(message);
        }
    }

    private static void printDebug(TranslationEntry te)
    {
        if (DBG)
        {
            VMKernel.printDebug(te);
        }
    }
}
