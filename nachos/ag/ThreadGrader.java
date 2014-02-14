package nachos.ag;
 
import java.util.HashSet;
import java.util.Hashtable;
import nachos.machine.Interrupt;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Stats;
import nachos.security.Privilege;
import nachos.threads.KThread;
import nachos.threads.Scheduler;
import nachos.threads.Semaphore;
import nachos.threads.ThreadedKernel;

public class ThreadGrader extends AutoGrader
{
	void run() 
	{
		
	}
	ThreadExtension getExtension(KThread thread)
	{
		ThreadExtension ext = (ThreadExtension)this.threadTable.get(thread);
		 if (ext != null) 
		 {
		   return ext;
		 }
		 return new ThreadExtension(thread);
	  }
  
	abstract class GeneralTestExtension
	{
		ThreadGrader.ThreadExtension ext;
	    GeneralTestExtension() {}
	}
 
	class ThreadExtension
	{
		KThread thread;
		String name;
     
		ThreadExtension(KThread thread)
		{
			this.thread = thread;
       
			this.name = thread.getName();
       
			ThreadGrader.this.threadTable.put(thread, this);
		}
    
		boolean finished = false;
		Semaphore joiner = new Semaphore(0);
		long readyTime;
		long sleepTime;
		long wakeTime;
		ThreadGrader.GeneralTestExtension addl = null;
	}
   
	Hashtable threadTable = new Hashtable();
	HashSet readySet = new HashSet();
	ThreadExtension current = null;
	KThread idleThread;
  
	public void setIdleThread(KThread idleThread)
	{
		super.setIdleThread(idleThread);
    
		this.idleThread = idleThread;
	}
   
	public void readyThread(KThread thread)
	{
		super.readyThread(thread);
    
		ThreadExtension ext = getExtension(thread);
     
		Lib.assertTrue(!this.readySet.contains(ext), "readyThread() called for thread that is already ready");
     
		Lib.assertTrue(!ext.finished, "readyThread() called for thread that is finished");
    
 
		ext.readyTime = this.privilege.stats.totalTicks;
		if (thread != this.idleThread)
		{
			this.readySet.add(ext);
		}
	}
   
	public void runningThread(KThread thread)
	{
		super.runningThread(thread);
    
		ThreadExtension prev = this.current;
		this.current = getExtension(thread);
		if ((prev != null) && (thread != this.idleThread)) 
		{
			Lib.assertTrue(this.readySet.remove(this.current), "runningThread() called for thread that was not ready");
		}
		Lib.assertTrue(!this.current.finished, "runningThread() called for thread that is finished");
	}
  
	public void finishingCurrentThread()
	{
		super.finishingCurrentThread();
		Lib.assertTrue(!this.current.finished, "finishCurrentThread() called for thread that is finished");
 
		this.current.finished = true;
		this.current.joiner.V();
    
		this.threadTable.remove(this.current.thread);
	}
   
	void delay(int ticks)
	{
		Lib.assertTrue(Machine.interrupt().enabled(), "internal error: delay() called with interrupts disabled");
		for (int i = 0; i < (ticks + 9) / 10; i++)
		{
			Machine.interrupt().disable();
			Machine.interrupt().enable();
		}
	}
   
	void y() {}
   
	void j(ThreadExtension ext)
	{
		ext.joiner.P();
	}
  
	ThreadExtension jfork(Runnable target)
	{
		return jfork(target, 1);
	}
  
	ThreadExtension jfork(Runnable target, int priority)
	{
		return jfork(target, priority, null);
	}
  
	ThreadExtension jfork(Runnable target, int priority, GeneralTestExtension addl)
	{
		KThread thread = new KThread(target);
		thread.setName("jfork");
    
		ThreadExtension ext = getExtension(thread);
		ext.addl = addl;
		if (addl != null) 
		{
			addl.ext = ext;
		}
		boolean intStatus = Machine.interrupt().disable();
   
		ThreadedKernel.scheduler.setPriority(thread, priority);
    
		thread.fork();
     
		Machine.interrupt().restore(intStatus);
     
		return ext;
	}
}

