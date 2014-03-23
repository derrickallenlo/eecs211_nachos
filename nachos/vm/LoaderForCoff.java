package nachos.vm;

import java.util.LinkedList;
import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

public class LoaderForCoff
{
    private static final int MAX_STACK_PAGES = 8;
    private static final boolean debugFlag = true;
    private final Coff coff;
    private final int PAGES_RESERVED_FOR_CODE;
    private final int[] SECTION_NUMBERS;
    private final int[] SECTION_OFFSET_NUMBERS;
    private final LinkedList<Integer> pagesUsedForStack;
    private static final byte[] ZERO_BUFFER = new byte[Processor.pageSize];

    //A lazy loader for code and stack pages
    public LoaderForCoff(Coff coff)
    {
        this.coff = coff;

        int count = 0;
        for (int i = 0; i < coff.getNumSections(); i++)
        {
            count += coff.getSection(i).getLength();
        }

        PAGES_RESERVED_FOR_CODE = count;

        printDebug("Initializing Coff loader with this manty pages for code: " + PAGES_RESERVED_FOR_CODE);
        SECTION_NUMBERS = new int[PAGES_RESERVED_FOR_CODE];
        SECTION_OFFSET_NUMBERS = new int[PAGES_RESERVED_FOR_CODE];

        //Reserve pages for each section of Coff
        //This saves us from a bug where we allocate a stack page in the middle of allocating a code pages
        //we no longer have to keep a reference for each page of code in a dynamic array
        for (int sectionNumber = 0; sectionNumber < coff.getNumSections(); sectionNumber++)
        {
            CoffSection section = coff.getSection(sectionNumber);
            for (int offsets = 0; offsets < section.getLength(); offsets++)
            {
                int virtualPage = section.getFirstVPN() + offsets;
                SECTION_NUMBERS[virtualPage] = sectionNumber;
                SECTION_OFFSET_NUMBERS[virtualPage] = offsets;
                printDebug("  Creating page for vpn: " + virtualPage + "sectNo: " + sectionNumber + " VPN off: " + offsets);
            }
        }

        pagesUsedForStack = new LinkedList<Integer>();
    }

    private void printDebug(String message)
    {
        if (debugFlag)
        {
            VMKernel.printDebug(message);
        }
    }

    //We have a list of cpn already stored for this class
    //Need to reference to an existing physical page when they become available
    public TranslationEntry loadData(int pid, int virtualPage, int physicalPage)
    {
        printDebug("Lazy Swap-> virtual page: " + virtualPage + ", physical page: " + physicalPage);
        TranslationEntry entry = new TranslationEntry(virtualPage, physicalPage, true, false, false, false);
        if (virtualPage < PAGES_RESERVED_FOR_CODE)
        {
            //code pages are always read-only
            //entry.readOnly = true;
            readCodeIntoPhysicalPage(virtualPage, physicalPage);
        }
        else
        {
            //should not be able to overflow the max number of stack pages
            if (!pagesUsedForStack.contains(physicalPage) && pagesUsedForStack.size() >= MAX_STACK_PAGES)
            {
                return null;
            }

            clearPhysicalPageForStack(physicalPage);
        }

        return entry;

    }

    /**
     * Loading code from the coff sections is taken and condensed from
     * UserProcess, so no need to worry much if it works
     *
     * @param virtualPage
     * @param physicalPage
     * @return
     */
    private void readCodeIntoPhysicalPage(int virtualPage, int physicalPage)
    {
        int sectionNo = SECTION_NUMBERS[virtualPage];
        int sectionOffset = SECTION_OFFSET_NUMBERS[virtualPage];

        CoffSection section = coff.getSection(sectionNo);

        //Have to create translation entry dynamically since need a physical page to write to
        printDebug("    Loading from code, section: " + section.getName() + ": " + sectionNo + ", with vpn offset: " + sectionOffset);
        section.loadPage(sectionOffset, physicalPage);
    }

    private void clearPhysicalPageForStack(int physicalPage)
    {
        printDebug("    Loading from stack...");
        StringBuilder sb = new StringBuilder();
        int pageStart = Processor.makeAddress(physicalPage, 0);
        sb.append("Page Start index: ").append(pageStart).append(", page end index: ");
        System.arraycopy(ZERO_BUFFER, 0, Machine.processor().getMemory(), pageStart, Processor.pageSize);
        printDebug("      Zeroed out ppn: " + physicalPage + ", :" + sb.toString());
    }

    public Coff getCoff()
    {
        return coff;
    }

}
