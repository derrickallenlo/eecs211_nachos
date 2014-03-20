package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.Machine;

/**
 * A <tt>ReplacementAlgorithm</tt> 
 * Abstract class 
 * need to initiate with any Page replacement algorithm
 */
public abstract class ReplacementAlgorithm
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
    public ReplacementAlgorithm()
    {
    	
    }


    /* this method will be defined by page replacement algorithm */
    /**
  	 * perform page replacement algorithm
  	 * @param non
  	 * @return a physical page number that will replace 
  	 */
    abstract int findSwappedPage();
    
    /**
  	 * get current page fault
  	 * @param non
  	 * @return int number of page faults 
  	 */
    abstract int getNumberPageFault();
    
    /**
  	 * get replacement algorithm name
  	 * @param non
  	 * @return String name 
  	 */
    abstract String getAlgorithmName();
    
    abstract void removePage(int ppn);

}