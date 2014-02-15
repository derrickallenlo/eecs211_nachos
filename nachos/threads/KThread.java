package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 * 
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an argument
 * when creating <tt>KThread</tt>, and forked. For example, a thread that
 * computes pi could be written as follows:
 * 
 * <p>
 * <blockquote>
 * 
 * <pre>
 * class PiRun implements Runnable {
 * 	public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * The following code would then create a thread and start it running:
 * 
 * <p>
 * <blockquote>
 * 
 * <pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre>
 * 
 * </blockquote>
 */
public class KThread 
{
	protected static final char dbgThread = 't';
	/**
	 * Additional state used by schedulers.
	 * @see nachos.threads.PriorityScheduler.ThreadState
	 */
	public Object schedulingState = null;

	/**
	 * The status of this thread. A thread can either be new (not yet forked),
	 * ready (on the ready queue but not running), running, or blocked (not on
	 * the ready queue and not running).
	 */
	private Status status = Status.STATUS_NEW;
	
	private String name = "(unnamed thread)";
	private Runnable target;
	private TCB tcb;

	private int id = numCreated++;  	//Unique identifier for this thread. Used to deterministically compare threads.
	private static int numCreated = 0;	// Number of times the KThread constructor was called
	private static ThreadQueue readyQueue = null;
	private static KThread currentThread = null;
	private static KThread toBeDestroyed = null;
	private static KThread idleThread = null;
	
	private static enum Status
	{
		STATUS_NEW (0),
		STATUS_READY (1),
		STATUS_RUNNING (2),
		STATUS_BLOCKED (3),
		STATUS_FINISHED (4);

//		private final int code;
//
//		public int getCode()
//		{
//			return code;
//		}

		private Status(int code)
		{
			//this.code = code;
		}
	}
	
	/**
	 * Get the current thread.
	 * 
	 * @return the current thread.
	 */
	public static KThread currentThread() 
	{
		Lib.assertTrue(currentThread != null);
		return currentThread;
	}

	/**
	 * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
	 * create an idle thread as well.
	 */
	public KThread() 
	{
		if (currentThread != null) 
		{
			tcb = new TCB();
		}
		else 
		{
			readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
			readyQueue.acquire(this);

			currentThread = this;
			tcb = TCB.currentTCB();
			name = "main";
			restoreState();

			createIdleThread();
		}
	}

	/**
	 * Allocate a new KThread.
	 * 
	 * @param target the object whose <tt>run</tt> method is called.
	 */

	public KThread(Runnable target) 
	{
		this();
		this.target = target;
	}

	/**
	 * Set the target of this thread.
	 * 
	 * @param target the object whose <tt>run</tt> method is called.
	 * @return this thread.
	 */
	public KThread setTarget(Runnable target) 
	{
		Lib.assertTrue(status == Status.STATUS_NEW);

		this.target = target;
		return this;
	}

	/**
	 * Set the name of this thread. This name is used for debugging purposes
	 * only.
	 * 
	 * @param name the name to give to this thread.
	 * @return this thread.
	 */
	public KThread setName(String name) 
	{
		this.name = name;
		return this;
	}

	/**
	 * Get the name of this thread. This name is used for debugging purposes
	 * only.
	 * 
	 * @return the name given to this thread.
	 */
	public String getName() 
	{
		return name;
	}

	/**
	 * Get the full name of this thread. This includes its name along with its
	 * numerical ID. This name is used for debugging purposes only.
	 * 
	 * @return the full name given to this thread.
	 */
	public String toString() 
	{
		return (name + " (#" + id + ")");
	}

	/**
	 * Deterministically and consistently compare this thread to another thread.
	 */
	public int compareTo(Object o) 
	{
		KThread thread = (KThread) o;

		if (id < thread.id)
			return -1;
		else if (id > thread.id)
			return 1;
		else
			return 0;
	}

	/**
	 * Causes this thread to begin execution. The result is that two threads are
	 * running concurrently: the current thread (which returns from the call to
	 * the <tt>fork</tt> method) and the other thread (which executes its
	 * target's <tt>run</tt> method).
	 */
	public void fork() 
	{
		Lib.assertTrue(status == Status.STATUS_NEW);
		Lib.assertTrue(target != null);

		Lib.debug(dbgThread, "Forking thread: " + toString() + " Runnable: "
				+ target);

		boolean intStatus = Machine.interrupt().disable();

		tcb.start(new Runnable() 
		{
			public void run()
			{
				runThread();
			}
		});

		ready();

		Machine.interrupt().restore(intStatus);
	}

	private void runThread() 
	{
		begin();
		target.run();
		finish();
	}

	private void begin() 
	{
		Lib.debug(dbgThread, "Beginning thread: " + toString());

		Lib.assertTrue(this == currentThread);

		restoreState();

		Machine.interrupt().enable();
	}

	/**
	 * Finish the current thread and schedule it to be destroyed when it is safe
	 * to do so. This method is automatically called when a thread's
	 * <tt>run</tt> method returns, but it may also be called directly.
	 * 
	 * The current thread cannot be immediately destroyed because its stack and
	 * other execution state are still in use. Instead, this thread will be
	 * destroyed automatically by the next thread to run, when it is safe to
	 * delete this thread.
	 */
	public static void finish() 
	{
		Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

		Machine.interrupt().disable();

		Machine.autoGrader().finishingCurrentThread();

		Lib.assertTrue(toBeDestroyed == null);
		toBeDestroyed = currentThread;

		currentThread.status = Status.STATUS_FINISHED;

		sleep();
	}

	/**
	 * Relinquish the CPU if any other thread is ready to run. If so, put the
	 * current thread on the ready queue, so that it will eventually be
	 * rescheduled.
	 * 
	 * <p>
	 * Returns immediately if no other thread is ready to run. Otherwise returns
	 * when the current thread is chosen to run again by
	 * <tt>readyQueue.nextThread()</tt>.
	 * 
	 * <p>
	 * Interrupts are disabled, so that the current thread can atomically add
	 * itself to the ready queue and switch to the next thread. On return,
	 * restores interrupts to the previous state, in case <tt>yield()</tt> was
	 * called with interrupts disabled.
	 */
	public static void yield() 
	{
		Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

		Lib.assertTrue(currentThread.status == Status.STATUS_RUNNING);

		boolean intStatus = Machine.interrupt().disable();

		currentThread.ready();

		runNextThread();

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Relinquish the CPU, because the current thread has either finished or it
	 * is blocked. This thread must be the current thread.
	 * 
	 * <p>
	 * If the current thread is blocked (on a synchronization primitive, i.e. a
	 * <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
	 * some thread will wake this thread up, putting it back on the ready queue
	 * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
	 * scheduled this thread to be destroyed by the next thread to run.
	 */
	public static void sleep() 
	{
		Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());

		if (currentThread.status != Status.STATUS_FINISHED)
		{
			currentThread.status = Status.STATUS_BLOCKED;
		}

		runNextThread();
	}
	
	public static void block()
	{
		boolean intStatus = Machine.interrupt().disable();
		sleep();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Moves this thread to the ready state and adds this to the scheduler's
	 * ready queue.
	 */
	public void ready() 
	{
		Lib.debug(dbgThread, "Ready thread: " + toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(status != Status.STATUS_READY);			// STATUS_NEW thread

		status = Status.STATUS_READY;
		if (this != idleThread)
		{
			readyQueue.waitForAccess(this);
		}

		Machine.autoGrader().readyThread(this);
	}
	
	public void wake()
	{
		boolean intStatus = Machine.interrupt().disable();
		ready();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Waits for this thread to finish. If this thread is already finished,
	 * return immediately. This method must only be called once; the second call
	 * is not guaranteed to return. This thread must not be the current thread.
	 */
	public void join() 
	{
		Lib.debug(dbgThread, "Joining to thread: " + toString());
		Lib.assertTrue(this != currentThread);
		
		boolean intStatus = Machine.interrupt().disable();//block others
            while (this.status != Status.STATUS_FINISHED)//make sure this thread must finish before leaving join()
            {
                if (currentThread != this)
                {
                    //if current thread is not this thread, current thread yield this thread
//                        if (currentThread.schedulingState instanceof PriorityScheduler.ThreadState)
//                        {
//                            PriorityScheduler.ThreadState currState = (PriorityScheduler.ThreadState) currentThread.schedulingState;
//                            PriorityScheduler.ThreadState myState = (PriorityScheduler.ThreadState) this.schedulingState;
//
//                            System.out.println(this.name + "Priority before: " + myState.effectivePriority);
//                            (currState).offerDonation(myState);
//                            System.out.println(this.name + "Priority after: " + myState.effectivePriority);
//
//                        }
                    ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
                    waitQueue.acquire(this);
                    waitQueue.waitForAccess(currentThread);
                    yield();
                    }
		}
		Machine.interrupt().restore(intStatus);//let other run if this thread finished
	}

	/**
	 * Create the idle thread. Whenever there are no threads ready to be run,
	 * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
	 * idle thread must never block, and it will only be allowed to run when all
	 * other threads are blocked.
	 * 
	 * <p>
	 * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
	 */
	private static void createIdleThread() 
	{
		Lib.assertTrue(idleThread == null);

		idleThread = new KThread(new Runnable() 
		{
			public void run() 
			{
				while (true)
					yield();
			}
		});
		idleThread.setName("idle");

		Machine.autoGrader().setIdleThread(idleThread);

		idleThread.fork();
	}

	/**
	 * Determine the next thread to run, then dispatch the CPU to the thread
	 * using <tt>run()</tt>.
	 */
	private static void runNextThread() 
	{
		KThread nextThread = readyQueue.nextThread();
		if (nextThread == null)
		{
			nextThread = idleThread;
		}
		nextThread.run();
	}

	/**
	 * Dispatch the CPU to this thread. Save the state of the current thread,
	 * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
	 * load the state of the new thread. The new thread becomes the current
	 * thread.
	 * 
	 * <p>
	 * If the new thread and the old thread are the same, this method must still
	 * call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
	 * <tt>restoreState()</tt>.
	 * 
	 * <p>
	 * The state of the previously running thread must already have been changed
	 * from running to blocked or ready (depending on whether the thread is
	 * sleeping or yielding).
	 * 
	 * @param finishing <tt>true</tt> if the current thread is finished, and
	 * should be destroyed by the new thread.
	 */
	private void run() 
	{
		Lib.assertTrue(Machine.interrupt().disabled());

		Machine.yield();

		currentThread.saveState();

		Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
				+ " to: " + toString());

		currentThread = this;

		tcb.contextSwitch();

		currentThread.restoreState();
	}

	/**
	 * Prepare this thread to be run. Set <tt>status</tt> to
	 * <tt>STATUS_RUNNING</tt> and check <tt>toBeDestroyed</tt>.
	 */
	protected void restoreState() 
	{
		Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
		Lib.assertTrue(tcb == TCB.currentTCB());

		Machine.autoGrader().runningThread(this);

		status = Status.STATUS_RUNNING;

		if (toBeDestroyed != null) 
		{
			toBeDestroyed.tcb.destroy();
			toBeDestroyed.tcb = null;
			toBeDestroyed = null;
		}
	}

	/**
	 * Prepare this thread to give up the processor. Kernel threads do not need
	 * to do anything here.
	 */
	protected void saveState() 
	{
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
	}

	//====================================================================================
	private static class PingTest implements Runnable 
	{
		private int which;

		PingTest(int which) 
		{
			this.which = which;
		}

		public void run() 
		{
			for (int i = 0; i < 5; i++) 
			{
				System.out.println("*** thread " + which + " looped " + i + " times");
				yield();
			}
		}

	}

	private static class HelloWorldTest implements Runnable 
	{
		private int which;
		private int priority;

		HelloWorldTest(int which, int priority) 
		{
			this.which = which;
			this.priority = priority;
		}

		public void run() 
		{
			for (int i = 0; i < 5; i++) 
			{
				System.out.println(" Count " + i +" Thread " + which + " is executing with Priority: " + priority + ", "+ currentThread.name );
				yield();
			}
		}

	}

	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() 
	{       
//            
//            	System.out.println("---------PriorityScheduler test---------------------");
//PriorityScheduler s = new PriorityScheduler();
//ThreadQueue queue = s.newThreadQueue(true);
//ThreadQueue queue2 = s.newThreadQueue(true);
//ThreadQueue queue3 = s.newThreadQueue(true);
//
//KThread thread1 = new KThread();
//KThread thread2 = new KThread();
//KThread thread3 = new KThread();
//KThread thread4 = new KThread();
//KThread thread5 = new KThread();
//thread1.setName("thread1");
//thread2.setName("thread2");
//thread3.setName("thread3");
//thread4.setName("thread4");
//thread5.setName("thread5");
//
//
//boolean intStatus = Machine.interrupt().disable();
//
//queue3.acquire(thread1);
//queue.acquire(thread1);
//queue.waitForAccess(thread2);
//queue2.acquire(thread4);
//queue2.waitForAccess(thread1);
//System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
//System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
//System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
//
//s.getThreadState(thread2).setPriority(3);
//
//System.out.println("After setting thread2's EP=3:");
//System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
//System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
//System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
//
//queue.waitForAccess(thread3);
//s.getThreadState(thread1).setPriority(5);
//
//System.out.println("After adding thread1 with EP=5:");
//System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
//System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
//System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
//System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
//
//s.getThreadState(thread1).setPriority(2);
//
//System.out.println("After setting thread1 EP=2:");
//System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
//System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
//System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
//System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
//
//System.out.println("Thread1 acquires queue and queue3");
//
//Machine.interrupt().restore(intStatus);
//System.out.println("--------End PriorityScheduler test------------------");

		boolean intStatus;
               /*****Priority Scheduler Self-Test*****/
               System.out.println("+---------------------------------+");
               System.out.println("+   Priority Scheduler Self Test  +");
               System.out.println("+---------------------------------+");
               PriorityScheduler s = new PriorityScheduler();
               ThreadQueue testQueue = s.newThreadQueue(true);
               
          
//               System.out.println("+----       Test Case 1      -----+");
//               System.out.println("+ Verify Threads run in Priority  +");
//               System.out.println("+---------------------------------+");
//               System.out.println("  ");
//               System.out.println("1) Create/Run 2 threads with default Priority 1");
//               KThread t1 =new KThread(new HelloWorldTest(1, 1)).setName("Thread 1");
//               KThread t2 =new KThread(new HelloWorldTest(2, 1)).setName("Thread 2");
//
              intStatus=  Machine.interrupt().disable();
//               testQueue.waitForAccess(t1);
//               testQueue.waitForAccess(t2);
//               testQueue.nextThread().fork();
//               testQueue.nextThread().fork();
//               yield();
//               runNextThread();
//               
//               
//               System.out.println("  ");
//               System.out.println("1) Create/Run 3 threads with multiple Priorities");
//               KThread t1b =new KThread(new HelloWorldTest(5, 5)).setName("Thread 5");
//               KThread t2b =new KThread(new HelloWorldTest(1, 1)).setName("Thread 1a");
//               KThread t3b =new KThread(new HelloWorldTest(4, 4)).setName("Thread 4");
//               KThread t4b =new KThread(new HelloWorldTest(13, 1)).setName("Thread 1b");
//               
//               s.setPriority(t1b, 5);
//               testQueue.acquire(t1b);
//               testQueue.waitForAccess(t2b);
//               s.setPriority(t3b, 4);
//               testQueue.acquire(t3b);
//               testQueue.waitForAccess(t4b);
//               Machine.interrupt().restore(intStatus);
//               
//               t1b.fork();
//               t2b.fork();
//               t3b.fork();
//               t4b.fork();
//               new HelloWorldTest(0, 1).run();
               
            //   yield();
            //   runNextThread();
               
               
     /*****Priority Scheduler Self-Test*****/
      System.out.println("+---------------------------------+");
      System.out.println("+   Priority Scheduler Self Test  +");
      System.out.println("+---------------------------------+");
      
 
      System.out.println("+----       Test Case 1      -----+");
      System.out.println("+ Verify Threads run in Priority  +");
      System.out.println("+---------------------------------+");
      System.out.println("  ");
      System.out.println("1) Create/Run 2 threads with default Priority 1");
      KThread t1 =new KThread(new HelloWorldTest(1, 1)).setName("Thread 1");
      KThread t2 =new KThread(new HelloWorldTest(2, 1)).setName("Thread 2");
      t1.fork();
      t2.fork();
      intStatus = Machine.interrupt().disable();
      readyQueue.print();
      Machine.interrupt().restore(intStatus);
      new HelloWorldTest(0, 1).run();
      
      System.out.println("  ");
      System.out.println("1) Create/Run 3 threads with multiple Priorities");
      KThread t1b =new KThread(new HelloWorldTest(5, 5)).setName("Thread 5");
      KThread t2b =new KThread(new HelloWorldTest(1, 1)).setName("Thread 1a");
      KThread t3b =new KThread(new HelloWorldTest(4, 4)).setName("Thread 4");
      KThread t4b =new KThread(new HelloWorldTest(13, 1)).setName("Thread 1b");
      
      intStatus = Machine.interrupt().disable();
      ThreadedKernel.scheduler.setPriority(t1b, 5);
      ThreadedKernel.scheduler.setPriority(t3b, 4);
      Machine.interrupt().restore(intStatus);
      
      t1b.fork();
//       intStatus = Machine.interrupt().disable();
//        readyQueue.acquire(t1b);
//      Machine.interrupt().restore(intStatus);
//         yield();
      t2b.fork();
      t3b.fork();
      t4b.fork();
      intStatus = Machine.interrupt().disable();
      readyQueue.print();
      Machine.interrupt().restore(intStatus);
      new HelloWorldTest(0, 1).run();
  
      
      
//             System.out.println("+----       Test Case 2      -----+");
//             System.out.println("+ Verify Threads run in Priority  +");
//             System.out.println("+---------------------------------+");
//             boolean intStatus = Machine.interrupt().disable();
//             System.out.println("  ");
//             System.out.println("1) Create/Run 3 threads with multiple Priorities");
//             KThread t5 =new KThread(new HelloWorldTest(5, 5)).setName("Thread 5");
//             KThread t1 =new KThread(new HelloWorldTest(1, 1)).setName("Thread 1");
//             
//             s.setPriority(t5, 5);
//             testQueue.waitForAccess(t5);
//             testQueue.waitForAccess(t1);
//             testQueue.nextThread().fork();
//             testQueue.nextThread().fork();
//             Machine.interrupt().restore(intStatus);
//             new PingTest(2).run();
//             t5.join();
             
           //  yield();
           //  runNextThread();
               
            
            /*
		Lib.debug(dbgThread, "Enter KThread.selfTest");
		KThread t1 =new KThread(new PingTest(1)).setName("forked thread");
		KThread t2 =new KThread(new PingTest(2)).setName("forked thread");
		t1.fork();
		new PingTest(0).run();
		
		
		//---------------------
		Alarm a = new Alarm();
		a.waitUntil(1000);
		//---------------------
		
		//join test
		t1.join();
		/*
		t2.fork();
		Communicator.selfTest();//communicator and condition test
		t2.join();
		
		Boat.selfTest();*/
		
	}

}
