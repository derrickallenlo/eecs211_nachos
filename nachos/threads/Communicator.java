package nachos.threads;

import java.util.LinkedList;


/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator 
{

	//all initializations
	private Lock conditionLock;//zero-length bounded buffer only one lock
	private Condition2  conditionSpeaker; 
	private Condition2  conditionListener; 
	private LinkedList<KThread> waitQueueSpeaker;//multiple speakers 
    private LinkedList<KThread> waitQueueListener;//multiple listeners
	private LinkedList<Integer> word;//word from Speaker to Listener
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() 
	{
		conditionLock = new Lock();
		conditionSpeaker = new Condition2(conditionLock); 
		conditionListener = new Condition2 (conditionLock); 
		waitQueueSpeaker = new LinkedList<KThread>();
		waitQueueListener = new LinkedList<KThread>();
		word = new LinkedList<Integer>();
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) 
	{
		conditionLock.acquire();
		this.word.add(word);
		//System.out.println("updated word");
		if (waitQueueListener.isEmpty())//no Listener
		{
			//wait in the queue until some Listener in the queue wakes this thread
			waitQueueSpeaker.add(KThread.currentThread());
			conditionSpeaker.sleep();//release lock and reacquire the lock after sleep
		}
		else
		{
			//wakes Listener and send word to Listener
			waitQueueListener.removeFirst();//dequeue
			conditionListener.wake();//speaker wakes listener	
		}
		conditionLock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() 
	{
		conditionLock.acquire();
		if (waitQueueSpeaker.isEmpty())//no Speaker
		{
			//System.out.println("listener is waiting");
			//wait in the queue until some Speaker in the queue wakes this thread
			waitQueueListener.add(KThread.currentThread());
			conditionListener.sleep();//release lock and reacquire the lock after sleep
		}
		else
		{
		//wakes Speaker get word from Speaker
			waitQueueSpeaker.removeFirst();//dequeue
			conditionSpeaker.wake();//listener wakes speaker
		}
		conditionLock.release();
		return word.removeFirst();
	}
}
