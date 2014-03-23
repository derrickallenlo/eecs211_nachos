package nachos.vm;

import java.util.ArrayList;
import java.util.Stack;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.threads.Semaphore;
import nachos.threads.ThreadedKernel;

public class SwapPageManager
{
    //Maintains a single global swap file, will be used by filesystem
    public static final String TEST_FILE = "Proj3SwapTestFile";

    private static int FREE_FRAMES_CREATTED = 0;
    private static OpenFile OPEN_TEST_FILE;
    private static Stack<Integer> RECYCLED_FRAMES;

    private SwapPageManager()
    {

    }

    public static void initialize()
    {
        //create new file and write null chars into it (clearing any prev data)
        OPEN_TEST_FILE = ThreadedKernel.fileSystem.open(TEST_FILE, true);
        byte[] zeroBuffer = new byte[Machine.processor().pageSize * Machine.processor().getNumPhysPages()];
        OPEN_TEST_FILE.write(zeroBuffer, 0, zeroBuffer.length);
        RECYCLED_FRAMES = new Stack<Integer>();
        SwapPageTable.initialize();
    }

    public static boolean accessSwapFile(SwapPage page, int ppn, boolean write)
    {
        int frameNumber = page.frameNumber; 
        int numberOfSucessfulBytes;
        byte[] memory = Machine.processor().getMemory();
        int index = frameNumber * Processor.pageSize;
        int bufferOffset = Processor.makeAddress(ppn, 0);
        if (frameNumber > FREE_FRAMES_CREATTED)
        {
            return false;
        }
        
        if (write)
        {
           numberOfSucessfulBytes = OPEN_TEST_FILE.write(index, memory, bufferOffset, Processor.pageSize);
        }
        else
        {
          numberOfSucessfulBytes  = OPEN_TEST_FILE.read(index, memory, bufferOffset, Processor.pageSize);
        }
        
        //some perro checking after access file
        if (-1 == numberOfSucessfulBytes)
        {
            // failed to find page?
            return false;
        }
        else
        {
            return numberOfSucessfulBytes == Processor.pageSize;
        }
    }

    // allocate a new SwapPage when Memory page first time swap out 
    public static SwapPage createSwapPage(MemoryPage memoryPage)
    {
        SwapPage swapPage = getSwapPage(memoryPage.getOwningProcessId(), memoryPage.entry.vpn);
        
        //allocate a new one if there is matching for the vpn
        if (null == swapPage)
        {
            int newFrame;
            if (RECYCLED_FRAMES.isEmpty())
            {
                newFrame = FREE_FRAMES_CREATTED++;
            }
            else
            {
                newFrame = RECYCLED_FRAMES.pop();
            }
            
            swapPage = new SwapPage(memoryPage, newFrame);
            SwapPageTable.put(memoryPage.getOwningProcessId(), memoryPage.entry.vpn, swapPage);
        }
        
        return swapPage;
    }

    // return the SwapPage referenced by pid and vpn in disk 
    public static SwapPage getSwapPage(int pid, int vpn)
    {
        return SwapPageTable.get(pid, vpn);
    }

    public static void removeSwapPage(int pid, int vpn)
    {
        SwapPage swapPage = SwapPageTable.remove(pid, vpn);
        if (swapPage != null)
        {
            //we can reuse the frame number from swap pages no longer used
            RECYCLED_FRAMES.push(swapPage.frameNumber);
        }
    }
    
        // close and unlink the file
    public static void closeTestFile()
    {
        OPEN_TEST_FILE.close();
        ThreadedKernel.fileSystem.remove(TEST_FILE);
    }

    //might as well make this private since we have getters
    private static class SwapPageTable
    {
        private static ArrayList<Integer> pidArray;
        private static ArrayList<Integer> vpnArray;
        private static ArrayList<SwapPage> swapPageArray;
        private static Semaphore sem;

        private SwapPageTable()
        {
            initialize();
        }

        private static void initialize()
        {
            pidArray = new ArrayList<Integer>();
            vpnArray = new ArrayList<Integer>();
            swapPageArray = new ArrayList<SwapPage>();
            sem = new Semaphore(1);

        }

        //can replace pages that are used already
        private static boolean put(Integer pid, Integer vpn, SwapPage sp)
        {
            int size = vpnArray.size();
            Lib.assertTrue(pidArray.size() == size && size == swapPageArray.size());
            sem.P();

            for (int i = 0; i < size; i++)
            {
                if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
                {

                    swapPageArray.set(i, sp);
                    sem.V();
                    return true;
                }

            }

            pidArray.add(pid);
            vpnArray.add(vpn);
            swapPageArray.add(sp);
            sem.V();
            return false;
        }

        private static SwapPage get(Integer pid, Integer vpn)
        {
            int size = vpnArray.size();
            Lib.assertTrue(pidArray.size() == size && size == swapPageArray.size());
            sem.P();
            for (int i = 0; i < size; i++)
            {
                if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
                {
                    sem.V();
                    return swapPageArray.get(i);
                }

            }
            sem.V();
            return null;
        }

        private static SwapPage remove(Integer pid, Integer vpn)
        {
            int size = vpnArray.size();
            Lib.assertTrue(pidArray.size() == size && size == swapPageArray.size());
            sem.P();
            for (int i = 0; i < size; i++)
            {
                if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
                {
                    SwapPage temp = swapPageArray.get(i);
                    pidArray.remove(i);
                    vpnArray.remove(i);
                    swapPageArray.remove(i);
                    sem.V();
                    return temp;
                }

            }
            sem.V();
            return null;
        }
    }

    public static class SwapPage
    {
        final int frameNumber;
        final MemoryPage memoryPage;

        public SwapPage(MemoryPage memoryPage, int frameNumber)
        {
            this.memoryPage = memoryPage;
            this.frameNumber = frameNumber;
        }

        public MemoryPage getDiskPage()
        {
            return memoryPage;
        }

        public int getFrameNumber()
        {
            return frameNumber;
        }
    }
}
