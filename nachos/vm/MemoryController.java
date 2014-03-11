package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

/**
 * A <tt>MemroyController</tt> 
 * 
 * that handle physical memory Control such as:
 * page swapping between physical memory and disk memory
 * remove/move pages if page has moved from/to disk memory but also keep track TLB 
 */
public class MemoryController 
{
	//getting ipt if need swap in to memory also update the ipt
	ReplacementAlgorithm pageReplacementAlgorithm;
	
	public MemoryController()
	{
		//initilize usedFrame
		pageReplacementAlgorithm = new SecondChanceReplacement();
	}
	/**
  	 * swap out physical page from physical memory 
  	 * 
  	 * @param ppn physical page number has to swap to disk memory
  	 */
	public void swapOut(int ppn)
	{
		MemoryPage swapOutPage = VMKernel.physicalMemoryMap[ppn];
		
		//make sure it's in the memory
		//if it's not in the memory, we don't need swap out
		if (swapOutPage != null && swapOutPage.entry.valid)
		{
			//if modified, update value at disk (ie. write() )
			//otherwist should not write any pages to the swap file
			//Your page-replacement policy should not write any pages to the swap file...
			if (swapOutPage.entry.dirty)
			{
				//TODO - Richard
				//update disk value with this entry
			}
			
			//TODO - Derrick 
			//make sure TLB doesn't keep this entry because it has been replaced under page replacement
				
			swapOutPage.entry.valid = false;
			VMKernel.invertedPageTable.remove(ppn);
			
		}
	}
	
	/**
  	 * find a page (page replacement algorithm) that has to swap out from physical memory 
  	 * and the missing page can swap in to physical memory
  	 * @param pid associated pid in inverted page table that vpn has not yet brought to memory
  	 * @param vpn missing virtual page number that has to swap in to physical memory
  	 */
	public TranslationEntry swapIn(int pid, int vpn)
	{
		
		
		int ppn = pageReplacementAlgorithm.findSwappedPage();
		swapOut(ppn);//if only if it's already in the memory
		
		//now perform Swap In
		//TODO - Richard
		//TranslationEntry entry = VMKernel.swapFile.getPage(pid, vpn); 
		TranslationEntry entry = null;
		
		VMKernel.invertedPageTable.put(pid^vpn, ppn);//update ipt
		MemoryPage newPage = new MemoryPage(pid, vpn, entry);
		VMKernel.physicalMemoryMap[ppn] = newPage;//update Core Map of tracking all ppn
		
		return entry;
	}
}
