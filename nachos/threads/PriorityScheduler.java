package nachos.threads;

import java.util.Comparator;

import nachos.machine.*;

import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler 
{
	//The default priority for a new thread. Do not change this value.
	public static final int DEFAULT_PRIORITY = 1;

	//The minimum priority that a thread can have. Do not change this value.
	public static final int MINIMUM_PRIORITY = 0;


	// The maximum priority that a thread can have. Do not change this value.
	public static final int MAXIMUM_PRIORITY = 7;


	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() 
	{
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) 
	{
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) 
	{
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= MINIMUM_PRIORITY
				&& priority <= MAXIMUM_PRIORITY);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() 
	{
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == MAXIMUM_PRIORITY)
		{
			ret = false;
		}
		else
		{
			setPriority(thread, priority + 1);
		}

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() 
	{
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == MINIMUM_PRIORITY)
		{
			ret = false;
		}
		else
		{
			setPriority(thread, priority - 1);
		}

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) 
	{
		if (thread.schedulingState == null)
		{
			thread.schedulingState = new ThreadState(thread);
		}

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue
	{
		/*
		 * Return  1 (positive int) if t2's priority is higher than t1
		 * Return -1 (negative int) if t1's priority is higher than t2
		 */
		private Comparator<ThreadState> threadComparator = new Comparator<ThreadState>()
		{
			@Override
			public int compare(ThreadState t1, ThreadState t2) 
			{
					if (t1.getEffectivePriority() < t2.getEffectivePriority())
					{
						return -1;
					}
					else if (t1.getEffectivePriority() == t2.getEffectivePriority()) 
					{
						if (t1.waitingTime >= t2.waitingTime)
						{
							return 1;
						}
						else
						{
							return -1;
						}
	
					}
					else 
					{
						return 1;
					}
			}
		};

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		private java.util.PriorityQueue<ThreadState> waitQueue;
		
		PriorityQueue(boolean transferPriority) 
		{
			this.transferPriority = transferPriority;
			waitQueue = new java.util.PriorityQueue<ThreadState>(10, threadComparator);
		}

		public void waitForAccess(KThread thread)
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		/**
		 * Notifies thread queue that it has received access
		 */
		public void acquire(KThread thread)
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		/**
		 * Removes highest priority thread from waitQueue
		 * 
		 * @return the next thread that has the highest priority
		 */
		public KThread nextThread() 
		{
			Lib.assertTrue(Machine.interrupt().disabled());

			if (waitQueue.isEmpty())
			{
				return null;
			}
			//System.out.print("\n Next Thread is: " + waitQueue.peek().thread.getName() + "\n");
			return waitQueue.remove().thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() 
		{
			return waitQueue.peek();
		}

		public void print() 
		{
			Lib.assertTrue(Machine.interrupt().disabled());

			for (Iterator<ThreadState> i = waitQueue.iterator(); i.hasNext();)
			{
				System.out.print(i.next() + " ");
			}
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState 
	{
		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;

		/** The effective priority of the associated thread. */
		protected int effectivePriority;

		/** The duration of time the thread has been waiting on waitQueue. */
		protected long waitingTime;
		private PriorityQueue waitQueue;

		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) 
		{
			this.thread = thread;
			this.waitingTime = Machine.timer().getTime();
			setPriority(DEFAULT_PRIORITY);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() 
		{
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() 
		{
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 */
		public void setPriority(int priority)
		{
			if (this.priority == priority)
				return;

			this.priority = priority;
			this.effectivePriority = priority;
		}
		
		/**
		 * If I no longer want an offered donation, I shall discard
		 */
		public void resetEffectivePriority()
		{
			effectivePriority = priority;
		}
		
		public void acceptDonation(int priority)
		{
			if (waitQueue.transferPriority && priority > effectivePriority)
			{
				effectivePriority = priority;
			}
		}
		
		/**
		 * If I am locked, I shall offer my priority as a donation
		 * @param otherThread
		 */
		public void offerDonation(ThreadState otherThread)
		{
			otherThread.acceptDonation(effectivePriority);
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) 
		{
			this.waitQueue = waitQueue;
			waitQueue.waitQueue.add(this);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) 
		{
			this.waitQueue = waitQueue;
			//TODO
			//waitQueue.waitQueue.remove(this);
		}
	}
}
