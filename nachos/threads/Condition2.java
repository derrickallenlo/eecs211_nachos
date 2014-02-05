package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
		//this.value = 0;
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		waitQueue.add(KThread.currentThread());//make current thread to waiting queue
		conditionLock.release();//release the lock
		
		//similar to P()
		boolean intStatus = Machine.interrupt().disable();	
		KThread.sleep();
		/*if (value == 0) 
		{
			KThread.sleep();//and go to sleep on this condition variable until another thread wakes it		
		}
		else
		{
			value--;
		}*/
		Machine.interrupt().restore(intStatus);
		conditionLock.acquire();// reqcauire the lock before return
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		if (!waitQueue.isEmpty())//make sure there is waiting thread
		{
			KThread thread = waitQueue.removeFirst();//remove waiting thread in the queue
			
			//similar to V()
			boolean intStatus = Machine.interrupt().disable();
			thread.ready();//make that thread to be ready. 
			Machine.interrupt().restore(intStatus);	
		}
			
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		while (!waitQueue.isEmpty())//dequeue all waiting thread from Linked List
		{
			wake();
		}
	}

	private Lock conditionLock;
	private LinkedList<KThread> waitQueue;
	//private int value;
	//private ThreadQueue swaitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
}
