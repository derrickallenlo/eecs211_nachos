package nachos.threads;

import java.util.LinkedList;

import nachos.ag.BoatGrader;

public class Boat
{
	static BoatGrader bg;
	
	//locks and conditions
	private static Lock lock;//who takes action...one at the time
	private static Condition conditionAdult;//Adult always follow what child did, only need one condition
	private static Condition conditionChildAtOahu;//children condition at Oahu
	private static Condition conditionChildAtMolokai;//children condition at Molokai
	
	private static int numAdultsAtOahu;//only ppl at Oahu can see
	private static int numChildrenAtOahu;//numChildrenAtOahu is not necessary but leave for readability
	private static int numAdultsAtMolokai;//only ppl at Molokai
	private static int numChildrenAtMolokai;
	private static int totalAdults;
	private static int totalChildren;
	
	private static LinkedList<KThread> oahuAdultQueue;//Oahu island that holds adults
	private static LinkedList<KThread> oahuChildrenQueue;//Oahu island that holds children
	private static LinkedList<KThread> molokaiChildrenQueue;//Molokai island that holds children
	
	private static String boatLocationStatus;
	private static final String boatAtOahu  = "Oahu";
	private static final String boatAtMolokai = "Molokai";
	
	private static boolean boatStatusPilotSeat;//false = not available, true = available 
	private static boolean boatStatusPassengerSeat;
	
	//waiting for passenger and pilot together
	private static LinkedList<KThread> boatChildrenQueue;//Molokai island that holds children
	private static Condition conditionChildAtBoat;//children condition on the boat
	
	private static Condition parentCondition;//for one-way communication from child threads
	
	/*public enum BoatLocation 
	{
	    OAHU, MOLOKAI
	}*/

	public static void selfTest() 
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b)
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		if (children <= 1)
		{
			System.out.println("Error: at least need two children");
		}
		bg = b;
		// Instantiate global variables here
		lock = new Lock();//who takes action...one at the time
		conditionAdult = new Condition(lock);
		conditionChildAtOahu = new Condition(lock);
		conditionChildAtMolokai = new Condition(lock);
		
		numAdultsAtOahu = children;//beginning all adults at Oahu
		numChildrenAtOahu = adults;
		numAdultsAtMolokai = 0;
		numChildrenAtMolokai = 0;
		
		oahuAdultQueue = new LinkedList<KThread>();
		oahuChildrenQueue = new LinkedList<KThread>();
		molokaiChildrenQueue = new LinkedList<KThread>();
		
		totalAdults = adults;
		totalChildren = children;
		
		//boat information initialization 
		boatLocationStatus = boatAtOahu;//boat initially at Oahu
		boatStatusPilotSeat = true;
		boatStatusPassengerSeat = true;

		boatChildrenQueue = new LinkedList<KThread>();
		conditionChildAtBoat = new Condition(lock);
		
		parentCondition = new Condition(lock);
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.

		/*Runnable r = new Runnable() {
			public void run() {
				SampleItinerary();
			}
		};
		KThread t = new KThread(r);
		t.setName("Sample Boat Thread");
		t.fork();*/
		
		lock.acquire();
		
		for (int i = 0; i < adults; i++)
		{
			Runnable r = new Runnable() 
			{
				public void run() 
				{
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Adult Thread");
			t.fork();
			
		}
		
		for (int i = 0; i < children; i++)
		{
			Runnable r = new Runnable() 
			{
				public void run() 
				{
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Child Thread");
			t.fork();
		}
		
		parentCondition.sleep();
		
		lock.release();

	}

	static void AdultItinerary()
	{
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		boolean atMolokai = false;
		
		lock.acquire();
		
		//still at Oahu
		if(!atMolokai)
		{
			//beginning all adults let child decide and all waiting at Oahu
			oahuAdultQueue.add(KThread.currentThread());
			conditionAdult.sleep();
			//when child wakes adult, get on the boat
			numAdultsAtOahu--;
			boatStatusPilotSeat = false;
			bg.AdultRowToMolokai();
			boatStatusPilotSeat = true;//arrived Molokai get off the seat
			boatLocationStatus = boatAtMolokai;
			numAdultsAtMolokai++;
			conditionChildAtMolokai.wake();//can only tell child at Molokai
		}
		//at Molokai this adult thread can be finished
	
		lock.release();
		
		
	}

	static void ChildItinerary()
	{	
		boolean atMolokai = false;
		
		lock.acquire();
		
		while(true)
		{
			//System.out.println("in while loop");
			//if(atMolokai)
			//{
			if(isFinished())//only at Molokai knows everyone arrive at Molokai
			{
				//System.out.println("finished");
				//wake up all waiting children at Molokai
				if(molokaiChildrenQueue.isEmpty())
				{
					parentCondition.wake();
					break;
				}
				else
				{
					molokaiChildrenQueue.removeFirst();
					conditionChildAtMolokai.wake();
				}
			}
				//atMolokai = childDecisionAndRun(atMolokai);
			//}
			else
			{
				//System.out.println("child at Oahu");
				atMolokai = childDecisionAndRun(atMolokai);
			}
		}
		//System.out.println("child done");
		lock.release();
		
	}
	static boolean isFinished()
	{
		int total = totalChildren + totalAdults; 
		if( (numAdultsAtMolokai + numChildrenAtMolokai) == total)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	static boolean childDecisionAndRun(boolean atMolokai)
	{
		if(atMolokai && boatLocationStatus == boatAtMolokai && (boatStatusPilotSeat || boatStatusPassengerSeat))
		{
			if(boatStatusPilotSeat)
			{
				boatStatusPilotSeat = false;//get on the pilot seat
				numChildrenAtMolokai--;
				bg.ChildRowToOahu();
				atMolokai = false;//arrived to Oahu
				numChildrenAtOahu++;
				boatLocationStatus = boatAtOahu;//boat arrived Oahu 
				boatStatusPilotSeat = true;//get off the pilot seat
				
				//now at Oahu
				if(numChildrenAtMolokai >= 1 && !oahuAdultQueue.isEmpty())//child remember number children at Molokai
				{
					//we should get an adult at Oahu
					oahuAdultQueue.removeFirst();
					conditionAdult.wake();
				}
				else
				{
					//we should get a child
					oahuChildrenQueue.removeFirst();
					conditionChildAtOahu.wake();
				}
				
			}
		}
		else if(!atMolokai && boatLocationStatus == boatAtOahu && (boatStatusPilotSeat || boatStatusPassengerSeat))
		{
			if(boatStatusPilotSeat)
			{
				boatStatusPilotSeat = false;//get on the pilot seat
				
				//only pilot child wake other child for the passenger
				if(!oahuChildrenQueue.isEmpty())
				{
					oahuChildrenQueue.removeFirst();
					conditionChildAtOahu.wake();
				}
				
				//waiting for child to get on the boat
				boatChildrenQueue.add(KThread.currentThread());
				conditionChildAtBoat.sleep();
				
				numChildrenAtOahu--;
				bg.ChildRowToMolokai();
				atMolokai = true;//arrived to Mololkai
				numChildrenAtMolokai++;
				boatLocationStatus = boatAtMolokai;//boat arrived Molokai together with pilot ...will execute twice for readability 
				boatStatusPilotSeat = true;//get off the pilot seat
				
				//now tell passenger that we are at Molokai
				boatChildrenQueue.removeFirst();
				conditionChildAtBoat.wake();

			}
			else//passenger action
			{
				boatStatusPassengerSeat = false;//get on the passenger seat
				numChildrenAtOahu--;
				
				//now tell pilot to row the boat
				boatChildrenQueue.removeFirst();
				conditionChildAtBoat.wake();
				
				//waiting for reaching the destination - Molokai
				boatChildrenQueue.add(KThread.currentThread());
				conditionChildAtBoat.sleep();
				
				bg.ChildRideToMolokai();
				atMolokai = true;//arrived to Mololkai
				numChildrenAtMolokai++;
				boatLocationStatus = boatAtMolokai;//boat arrived Molokai together with pilot
				boatStatusPassengerSeat = true;//get off the passenger seat
				
				//passenger child wake other child for next decision
				if(!molokaiChildrenQueue.isEmpty())
				{
					molokaiChildrenQueue.removeFirst();
					conditionChildAtMolokai.wake();
				}
			}
		}
		
		//no seat available or finished the decision 
		if(atMolokai)
		{
			molokaiChildrenQueue.add(KThread.currentThread());
			conditionChildAtMolokai.sleep();
		}
		else
		{
			oahuChildrenQueue.add(KThread.currentThread());
			conditionChildAtOahu.sleep();
		}
		
		return atMolokai;
	}
	static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
