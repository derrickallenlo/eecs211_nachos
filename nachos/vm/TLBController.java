package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

/**
 *
 * @author derrick
 */
public class TLBController 
{
    public void addEntry(TranslationEntry entry)
    {
        //Search for invalidEntries and replace first one that is found
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            if (!Machine.processor().readTLBEntry(i).valid) 
            {
                Machine.processor().writeTLBEntry(i, entry);
                return;
            }
        }
             
        //If table is full..
        Machine.processor().writeTLBEntry(Lib.random(Machine.processor().getTLBSize()), entry);
        
        //Search for oldest and replace
        //TODO
    }
    public void flush()
    {
        //Write all TLB entries back to the Page Table
        for (int i = 0; i < Machine.processor().getTLBSize(); i++)
        {
            TranslationEntry currentEntry = Machine.processor().readTLBEntry(i);
            
            if (currentEntry.valid)
            {
                VMKernel.physicalMemoryMap[currentEntry.ppn].entry = currentEntry;
            }
        }
    }
    
    //clear all TLB entries by invalidating all entries
    public void clear()
    {
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) 
        {
                TranslationEntry entry = Machine.processor().readTLBEntry(i);
		if (entry.valid)
                {
                        entry.valid = false;
			VMKernel.physicalMemoryMap[entry.ppn].setTranslationEntry(entry);
                        Machine.processor().writeTLBEntry(i, entry);
                }
        }
    }
}
