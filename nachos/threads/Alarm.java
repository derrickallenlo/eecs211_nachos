package nachos.threads;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm 
{
	private KThread k;
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() 
	{
		Machine.timer().setInterruptHandler(new Runnable() 
		{
			public void run() 
			{
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() 
	{
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) //TODO
	{
		long wakeTime = getTime() + x;
		
		//Save state of thread
		k = KThread.currentThread();
		boolean intStatus = Machine.interrupt().disable();
		
		//zzz...
		AlarmWaker aw = new AlarmWaker(k, wakeTime);
		aw.run();
		System.out.println("2");
		KThread.sleep();
		
		//Restore to running
		Machine.interrupt().restore(intStatus);
	}
	
	public static long getTime()
	{
	 return Machine.timer().getTime() ;	
	}
	
	private static class AlarmWaker implements Runnable 
	{
		KThread k;
		long wakeTime;
		
		private AlarmWaker(KThread k, long wakeTime)
		{
			this.k = k;
			this.wakeTime = wakeTime;
		}
		
		public void run() 
		{
			while (wakeTime > getTime());
			k.ready();
			System.out.println("1");
		}
		
	
	}
	
	//A thread calls waitUntil to suspend its own execution until time has advanced to at least now + x.
//	There is no requirement that threads start running
//	immediately after waking up; just put them on the ready queue in the timer interrupt handler after
//	they have waited for at least the right amount of time. Do not fork any additional threads to
//	implement waitUntil(); you need only modify waitUntil() and the timer interrupt
//	handler. waitUntil is not limited to one thread; any number of threads may call it and be
//	suspended at any one time.
}
