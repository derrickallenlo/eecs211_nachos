package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.threads.ThreadedKernel;

public class SwapPageManager
{		
    public static final String fileName = "SwapFile";

    private static int FREE_FRAMES_CREATTED;
    private static OpenFile file;
    private static LinkedList<Integer> RECYCLED_FRAMES;
    private static Hashtable<Integer, SwapPage> SWAP_MAP; //pin^vpn

    // Constructor 
    private SwapPageManager()
    {
        
    }
    
    public static void initialize()
    {
        FREE_FRAMES_CREATTED = 0;
        file = ThreadedKernel.fileSystem.open(fileName, true);
        byte[] b = new byte[Processor.pageSize * Machine.processor().getNumPhysPages()];
        file.write(b, 0, b.length);
        
        RECYCLED_FRAMES = new LinkedList<Integer>();
        SWAP_MAP = new Hashtable<Integer, SwapPage>();
    }

    // close the file
    public static void close()
    {
        file.close();
        ThreadedKernel.fileSystem.remove(fileName);
    }

    // write to the swap frame
    public static boolean write(int frameNumber, byte[] buf, int offset, int length)
    {
        if (frameNumber > FREE_FRAMES_CREATTED)
        {
            return false;
        }
        int numBytes = file.write(frameNumber * Processor.pageSize, buf, offset, length);
        if (numBytes == -1)
        {
            // TODO kill process here?
            return false;
        }
        else if (numBytes != Processor.pageSize)
        {
            return false;
        }
        else
        {
            return true;
        }

    }

    // read the swap frame
    public static boolean read(int frameNumber, byte[] buf, int offset, int length)
    {
        if (frameNumber > FREE_FRAMES_CREATTED)
        {
            return false;
        }
        int numBytes = file.read(frameNumber * Processor.pageSize, buf, offset, length);
        if (numBytes == -1)
        {
            // TODO kill process here?
            return false;
        }
        else return numBytes == Processor.pageSize;
    }

    // allocate a new SwapPage when Memory page first time swap out 
    public static SwapPage newSwapPage(MemoryPage memoryPage)
    {
        SwapPage swapPage = SWAP_MAP.get(memoryPage.processId ^ memoryPage.entry.vpn);
        if (swapPage == null)
        {
            int newPage;
            if (RECYCLED_FRAMES.isEmpty())
            {
                newPage = FREE_FRAMES_CREATTED++;
            }
            else
            {
                newPage = RECYCLED_FRAMES.removeFirst();
            }
            swapPage = new SwapPage(memoryPage, newPage);
            SWAP_MAP.put(memoryPage.processId ^ memoryPage.entry.vpn, swapPage);
        }
        return swapPage;
    }

    // return the SwapPage referenced by pid and vpn in disk 
    public static SwapPage getSwapPage(int pid, int vpn)
    {
        return SWAP_MAP.get(pid ^ vpn);
    }

    // delete a SwapPage 
    public static boolean deleteSwapPage(int pid, int vpn)
    {
        SwapPage swapPage = getSwapPage(pid, vpn);
        if (swapPage == null)
        {
            return false;
        }
        RECYCLED_FRAMES.add(swapPage.frameNumber);
        return true;
    }

    // return open file
    public static OpenFile getSwapFile()
    {
        return file;
    }

    public static int getMaxPossibleOfFreeFrames()
    {
        return FREE_FRAMES_CREATTED;
    }
    
}
