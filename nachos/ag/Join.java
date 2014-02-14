package nachos.ag;
 
import nachos.machine.Lib;
import nachos.threads.KThread;
 
public class Join extends ThreadGrader
{
	void run()
	{
		super.run();
		boolean joinAfterFinish = getBooleanArgument("joinAfterFinish");
		ThreadGrader.ThreadExtension t = jfork(new Runnable()
		{
			public void run() {}
		});
		if (joinAfterFinish) 
		{
			j(t);
		}
		t.thread.join();
    
		Lib.assertTrue(t.finished, "join() returned but target thread is not finished");

     done();
     
  }
}
