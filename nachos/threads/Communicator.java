package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;


/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
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
	public void speak(int word) {
		conditionLock.acquire();
		if (waitQueueListener.isEmpty())//no Listener
		{
			//wait in the queue until some Listener in the queue wakes this thread
			waitQueueSpeaker.add(KThread.currentThread());
			conditionSpeaker.sleep();//release lock and reacquire the lock after sleep
		}
		else
		{
			//wakes Listener and send word to Listener
			waitQueueListener.getFirst();//dequeue
			conditionListener.wake();//speaker wakes listener	
		}
		this.word=word;
		conditionLock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		conditionLock.acquire();
		if (waitQueueSpeaker.isEmpty())//no Speaker
		{
			//wait in the queue until some Speaker in the queue wakes this thread
			waitQueueListener.add(KThread.currentThread());
			conditionListener.sleep();//release lock and reacquire the lock after sleep
			
		}
		else
		{
		//wakes Speaker get word from Speaker
			waitQueueSpeaker.getFirst();//dequeue
			conditionSpeaker.wake();//listener wakes speaker
		}
		conditionLock.release();
		return this.word;
	}
	
	/*test communicator*/
	/*
	public static void selfTest() 
	{
		Communicator commu = new Communicator();
		
		KThread listenerThread = new KThread(new ListenTest(commu));
		KThread speakerThread = new KThread(new SpeakTest(commu,26));
		
		listenerThread.fork();
		speakerThread.fork();
		listenerThread.join();
		speakerThread.join();
	}
	private static class ListenTest implements Runnable {
		ListenTest(Communicator oldcommu) 
		{
			commu = oldcommu;
		}
		
		public void run() 
		{
			System.out.println("Listener is listening");
			int message = commu.listen();
			System.out.println("Listener finished and word is " + message);
		}

		private Communicator commu;
	}
	private static class SpeakTest implements Runnable {
		SpeakTest(Communicator oldcommu,int word) 
		{
			commu = oldcommu;
			this.word = word;
		}
		
		public void run() 
		{
			System.out.println("Speaker is speaking");
			commu.speak(word);
			System.out.println("Speaker finished");
		}

		private Communicator commu;
		private int word;
	}
	*/
	
	private Lock conditionLock;//zero-length bounded buffer only one lock
	private Condition2  conditionSpeaker; 
	private Condition2  conditionListener; 
	private LinkedList<KThread> waitQueueSpeaker;//multiple speakers 
    private LinkedList<KThread> waitQueueListener;//multiple listeners
	private int word;//word from Speaker to Listener
}
