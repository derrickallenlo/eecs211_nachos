package nachos.vm;

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
 * remove/move pages (ie. TLB)
 */
public class MemoryController 
{
	ReplacementAlgorithm pageReplacementAlgorithm = new SecondChanceReplacement(VMKernel.invertedPageTable);
	private LinkedList<Integer> freeFrames = new LinkedList<Integer>();
	int totalPhysicalPages;
	
	public MemoryController()
	{
		totalPhysicalPages = Machine.processor().getNumPhysPages();
		for (int i = 0; i < totalPhysicalPages; i++)
		{
			freeFrames.add(i);
		}
	}
	/**
  	 * swap out physical page from physical memory 
  	 * 
  	 * @param ppn physical page number has to swap to disk memory
  	 */
	public void swapOut(int ppn)
	{
		
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
		swapOut(ppn);
		
		//now perform Swap In
		//TODO - Richard
		//TranslationEntry entry  =VMKernel.swapFile.getPage(pid, vpn); 
		
		return null;
	}
}
