package nachos.vm;

import nachos.machine.TranslationEntry;

/**
 *
 * @author derrick
 */
public class MemoryPage 
{
    int processId;
    int virtualPageNumber;
    TranslationEntry entry;

    public MemoryPage(int processId,int virtualPageNumber, TranslationEntry entry) 
    {
            this.processId = processId;
            this.virtualPageNumber = virtualPageNumber;
            this.entry = entry;
    }
    
    public void setTranslationEntry(TranslationEntry entry)
    {
        this.entry = entry;
    }
}
