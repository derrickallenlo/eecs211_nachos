package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.Machine;

/**
 * A <tt>SecondChanceReplacement</tt> 
 * The second-chance algorithm must maintain a pointer similar to the FIFO algorithm. 
 * In addition, it needs an array of u-bits, one for each frame.
 * 
 * The Nachos TLB sets the dirty and used bits, which you can use to implement the clock
 * algorithm for page replacement. Alternately, you may choose to implement the nth chance
 * clock algorithm as described in the lecture notes (see the textbook for more details on
 * these algorithms).
 */


public class SecondChanceReplacement extends ReplacementAlgorithm
{
	
	 /*
     * ******************************************************
     * variables need from VMKernel, Machine.processor*******
     * ******************************************************
     * VMKernel.invertedPageTable:
     * TLB used hashTable to maintain which page resides in which frame.
     * ******************************************************
     * Machine.processor().getNumPhysPages() :
     * number of pages of physical memory in this simulated processor
     * ******************************************************
     * VMKernel.physicalMemoryMap[i].entry.used:
     * number of pages of physical memory in this simulated processor
     * ******************************************************
     * */
	private final String algorithmName = "Second Chance";
    private int current_frame;//pointer point to the location to be replace
    private int num_faults;
    private int used_frames;

    public SecondChanceReplacement()
    {
        super();
        current_frame = 0;
        num_faults = 0;
        used_frames = 0;
    }

    /**
     * find a page to be replace when page fault occurred
     *
     * @return
     */
    @Override
    public int findSwappedPage()
    {
    	num_faults++;
    	
        if (used_frames < Machine.processor().getNumPhysPages())
        {
        	//used bit set to true will happen in Processor.translate()
            return used_frames++;
        }
        else
        {
            /* evict page pointed to by current_frame and if only if its u-bit is false, replace with new page, and increment. */
            while (VMKernel.physicalMemoryMap[current_frame].entry.used)//search for used-bit contain 0
            {
            	VMKernel.physicalMemoryMap[current_frame].entry.used = false;

                current_frame = ++current_frame % Machine.processor().getNumPhysPages();//advance next frame
            }
            //used bit set to true will happen in Processor.translate()
            // remove existing page->frame mapping will happen in MemoryController swapIn
            // set new page->frame mapping will happen in MemoryController swapIn
            // update frame with the new page will happen in MemoryController swapIn

            //after the frames are full check the replacement
            /* advance current frame. */
            current_frame++;
            current_frame %= Machine.processor().getNumPhysPages();
            return current_frame;
        }
    }
    
    /*returns the number of faults. */
    public int getNumberPageFault()
    {
        return num_faults;
    }
    
    public String getAlgorithmName()
    {
    	return algorithmName;
    }
}