package nachos.threads;

import nachos.machine.*;

/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel 
{
	public static Scheduler scheduler = null;
	public static Alarm alarm = null;
	public static FileSystem fileSystem = null;

	// dummy variables to make javac smarter
	private static RoundRobinScheduler dummy1 = null;
	private static PriorityScheduler dummy2 = null;
	private static LotteryScheduler dummy3 = null;
	private static Condition2 dummy4 = null;
	private static Communicator dummy5 = null;
	private static Rider dummy6 = null;
	private static ElevatorController dummy7 = null;
	
	/**
	 * Allocate a new multi-threaded kernel.
	 */
	public ThreadedKernel() 
	{
		super();
	}

	/**
	 * Initialize this kernel. Creates a scheduler, the first thread, and an
	 * alarm, and enables interrupts. Creates a file system if necessary.
	 */
	public void initialize(String[] args) 
	{
		//TODO do all kernal initialize here
		// set scheduler
		String schedulerName = Config.getString("ThreadedKernel.scheduler");
		scheduler = (Scheduler) Lib.constructObject(schedulerName);

		// set fileSystem
		String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
		if (fileSystemName != null)
			fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
		else if (Machine.stubFileSystem() != null)
			fileSystem = Machine.stubFileSystem();
		else
			fileSystem = null;

		// start threading
		new KThread(null);

		alarm = new Alarm();

		Machine.interrupt().enable();
	}

	/**
	 * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
	 * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
	 * autograder never calls this method, so it is safe to put additional tests
	 * here.
	 */
	public void selfTest() 
	{
		//ONLY MODIFY FOR DEBUGGING
		KThread.selfTest();
		//Semaphore.selfTest();
		//SynchList.selfTest();
		if (Machine.bank() != null) 
		{
			ElevatorBank.selfTest();
		}
	}

	/**
	 * A threaded kernel does not run user programs, so this method does
	 * nothing.
	 */
	public void run() 
	{
		//DO NOT PUT CODE HERE
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() 
	{
		Machine.halt();
	}


}
