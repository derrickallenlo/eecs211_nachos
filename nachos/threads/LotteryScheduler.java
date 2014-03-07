package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler 
{
	public static final int MINIMUM_PRIORITY = 1;
	public static final int DEFAULT_PRIORITY = MINIMUM_PRIORITY;
	public static final int MAXIMUM_PRIORITY = Integer.MAX_VALUE;

	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() 
	{


	}

	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * tickets from waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	@Override
	public ThreadQueue newThreadQueue(boolean transferPriority) 
	{
		return new LotteryQueue(transferPriority);
	}
        
        @Override
	public void setPriority(KThread thread, int priority) 
	{
		setPriority( thread,  priority,  MINIMUM_PRIORITY,  MAXIMUM_PRIORITY) ;
	}

        @Override
	public boolean increasePriority() 
	{
		return increasePriority(MAXIMUM_PRIORITY) ;
	}

        @Override
	public boolean decreasePriority() 
	{
		return decreasePriority(MINIMUM_PRIORITY) ;
	}
        
	@Override
	protected ThreadState getThreadState(KThread thread) 
	{
		if (thread.schedulingState == null)
		{
			thread.schedulingState = new ThreadState(thread);
		}

		return (ThreadState) thread.schedulingState;
	}

	protected class LotteryQueue extends ThreadQueue
	{
		ArrayList<Integer> maxList;
		Random rand;
		public boolean transferPriority;
		private ArrayList<ThreadState> queue;
		protected ThreadState activeThreadState;

		public LotteryQueue (boolean transferPriority) 
		{
			this.transferPriority = transferPriority;
			queue = new ArrayList<ThreadState>();
			rand = new Random();
			maxList = new ArrayList<Integer>();
		}

		          @Override
            public KThread nextThread()
            {
                boolean debug = false;
                Lib.assertTrue(Machine.interrupt().disabled());
                ThreadState winner = null;

                if (queue.isEmpty())
                {
                    return null;
                }

                if (debug)  //ONLY FOR DEBUG! checking priority #'s correct
                {
                    for (ThreadState t : queue)
                    {
                        if (winner == null)
                        {
                            winner = t;
                        }
                        else
                        {
                            if (t.getEffectivePriority() > winner.getEffectivePriority())
                            {
                                winner = t;
                            }
                        }
                    }
                    
                }
                else
                {
                    /**
                     * Based on the number tickets owned per thread, give each
                     * thread that many tickets then randomly choose a ticket
                     * and find the winner!
                     *
                     * Example) Threads have this many tickets: 1 1 3 4 1 1 2 1.
                     * Create an Array of [1 2 5 9 10 11 13] 2. Randomly choose
                     * winning ticket #9 -> 5th thread wins! 3. Randomly choose
                     * winning ticket #0 -> 1st thread wins! Note: Ticket #13
                     * can never be chosen!
                     */
                    maxList.clear();
                    int counter = 0;
                    for (ThreadState t : queue)
                    {
                        counter += t.getEffectivePriority();
                        maxList.add(counter);
                    }

                    int winningTicket = rand.nextInt(counter);
                    for (int i = 0; i < maxList.size(); i++)
                    {
                        if (maxList.get(i) > winningTicket)
                        {
                            winner = queue.get(i);
                            break;
                        }
                    }
                }

                // =========================================
                queue.remove(winner);
                setActiveThread(winner);
                winner.listOfQueues.remove(this);
                
                return winner.getThread();
            }
                
		@Override
		public void waitForAccess(KThread thread)
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
		@Override
		public void acquire(KThread thread)
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}
		@Override
		public void print() 
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			System.out.println("  Start");
                                System.out.print("     Head: " + activeThreadState);
			for (Iterator<ThreadState> i = queue.iterator(); i.hasNext();)
			{
				System.out.print("        "+i.next());
			}
			System.out.println("  End");

		}

		protected void setActiveThread(ThreadState thread)
		{
			this.activeThreadState = thread;
		}

		protected ThreadState getActiveThread() 
		{
			return activeThreadState;
		}

		

	}

	private static LinkedList<ThreadState> deadlockKiller;
	protected class ThreadState extends PriorityScheduler.ThreadState
	{
		private ArrayList<LotteryQueue> listOfQueues ;
		public ThreadState(KThread thread) 
		{
			super(thread);
		}

                @Override
		public void setPriority(int priority)
		{
                    if (null == listOfQueues)
                    {
                        listOfQueues = new ArrayList<LotteryQueue> ();
                    }
			if (this.priority == priority)
			{
				return;
			}
                        
                        if (effectivePriority <= 0)
                        {
                            effectivePriority = priority;
                        }
			
			startTransfers(priority - effectivePriority, this);
			this.priority = priority;
		}
		
		private void transferTickets(int tickets, ThreadState recipient)
		{
			deadlockKiller.add(recipient);

			for(LotteryQueue queue: recipient.listOfQueues)
			{
				if (queue.transferPriority && !queue.queue.isEmpty())
				{
					ThreadState activeThread = queue.activeThreadState;
					if(recipient != activeThread && !deadlockKiller.contains(activeThread))
					{
						transferTickets(tickets, activeThread);
					}
				}
			}

			//should give the delta of ticket changes (negative or positive)
			recipient.effectivePriority = (recipient.effectivePriority + tickets);
		}
		
		 private void startTransfers(int ticketsToGive, ThreadState masterThread)
		 {
                     if (ticketsToGive == 0)
                     {
                         return;
                     }
			 if (deadlockKiller == null)
			 {
				 deadlockKiller = new LinkedList<ThreadState>();
			 }
			 transferTickets(ticketsToGive, masterThread);
			 deadlockKiller.clear();
		 }

            public void waitForAccess(LotteryQueue waitQueue)
            {
                waitingTime = Machine.timer().getTime();

                if ( waitQueue.transferPriority)
                {
                    {
                        //TODO not sure if this is okay or not? It's because there's no 'head' in a loterry queue
                        if (waitQueue.getActiveThread() == null)
                        {
                          //  acquire(waitQueue);
                        }
                        else
                        {
                            startTransfers(effectivePriority, waitQueue.getActiveThread());
                        }
                    }

                }

                waitQueue.queue.add(this);
                if (!listOfQueues.contains(waitQueue))
                {
                    listOfQueues.add(waitQueue);
                }
            }

		public void acquire(LotteryQueue waitQueue)
		{
			Lib.assertTrue(waitQueue.queue.isEmpty());
			waitQueue.setActiveThread(this);
		}
	}
}
