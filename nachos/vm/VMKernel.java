package nachos.vm;

import java.util.Hashtable;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel 
{
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
        public static Hashtable invertedPageTable;
        public static Lock invertedPageTableLock;
        
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() 
        {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) 
        {
		super.initialize(args); 
                initializeInvertedPageTable();
                
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() 
        {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() 
        {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() 
        {
		super.terminate();
	}
        
        private void initializeInvertedPageTable()
        {
            invertedPageTable = new Hashtable();
            invertedPageTableLock = new Lock();
            
        }
        
        //TLB Manager? To manage replacement policy
}
