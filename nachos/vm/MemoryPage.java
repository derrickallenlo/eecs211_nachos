package nachos.vm;

import nachos.machine.TranslationEntry;

/**
 *
 * @author derrick
 */
public class MemoryPage
{
    private final int processId;
    private final int virtualPageNumber;
    public TranslationEntry entry;

    public MemoryPage(int processId, int virtualPageNumber, TranslationEntry entry)
    {
        this.processId = processId;
        this.virtualPageNumber = virtualPageNumber;
        this.entry = entry;
    }

    public void setTranslationEntry(TranslationEntry entry)
    {
        this.entry = entry;
    }

    public TranslationEntry getTEntry()
    {
        return entry;
    }

    public int getOwningProcessId()
    {
        return processId;
    }

    public int getVirtualPageNumber()
    {
        return virtualPageNumber;
    }
}
