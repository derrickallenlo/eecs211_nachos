package nachos.ag;
 
import nachos.machine.Lib;
import nachos.threads.KThread;
import nachos.threads.Communicator;
 
public class CommunicatorTest extends ThreadGrader
{
	public static int message;
	
	void run()
	{
		super.run();
		int Speaker1 = getIntegerArgument("Speaker1");
		int Speaker2 = getIntegerArgument("Speaker2");
		selfTest(Speaker1,Speaker2);

		done();
     
  }
	
	private static class ListenTest implements Runnable 
	{
		private Communicator commu;
		;
		ListenTest(Communicator oldcommu) 
		{
			commu = oldcommu;
		}
		
		public void run() 
		{
			//System.out.println("Listener is listening");
			message = commu.listen();
			//System.out.println("Listener finished and word is " + message);
		}
	}
	private static class SpeakTest implements Runnable 
	{
		SpeakTest(Communicator oldcommu,int word) 
		{
			commu = oldcommu;
			this.word = word;
		}
		
		public void run() 
		{
			//System.out.println("Speaker is speaking");
			commu.speak(word);
			//System.out.println("Speaker finished");
		}

		private Communicator commu;
		private int word;
	}
	public static void selfTest(int Speaker1, int Speaker2) 
	{
		Communicator commu = new Communicator();
		
		KThread listenerThread = new KThread(new ListenTest(commu));
		KThread listenerThread2 = new KThread(new ListenTest(commu));
		KThread speakerThread = new KThread(new SpeakTest(commu,Speaker1));
		KThread speakerThread2 = new KThread(new SpeakTest(commu,Speaker2));
		
		speakerThread.fork();
		
		listenerThread.fork();	
		
		listenerThread.join();
		speakerThread.join();
		Lib.assertTrue((message == Speaker1), "Listener1 returned but the value is not match");
		
		listenerThread2.fork();
		speakerThread2.fork();
		listenerThread2.join();
		speakerThread2.join();
		Lib.assertTrue((message == Speaker2), "Listener2 returned but the value is not match");
	}
}
