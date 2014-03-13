package nachos.vm;

import java.util.Hashtable;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.threads.ThreadedKernel;

public class SwapFile
{
	//TODO 
    //assigned to Richard
    //The inverted page table must only contain entries for pages that are actually in physical
    //memory. You will need to maintain a separate data structure for locating pages in swap

	//If a process experiences an I/O error when accessing the swap file, you should kill the
    //process.			
    public static final String fileName = "SwapFile";

    private int size;
    private OpenFile file;
    private LinkedList<Integer> frames = new LinkedList<Integer>();
    private Hashtable<Integer, SwapPage> swapPageTable = new Hashtable<Integer, SwapPage>(); //pin^vpn

    // Constructor 
    public SwapFile()
    {
        size = 0;
        file = ThreadedKernel.fileSystem.open(fileName, true);
        byte[] buf = new byte[Processor.pageSize * Machine.processor().getNumPhysPages()];
        file.write(buf, 0, buf.length);
    }

    // close the file
    public void close()
    {
        file.close();
        ThreadedKernel.fileSystem.remove(fileName);
    }

    // write to the swap frame
    public boolean write(int frameNumber, byte[] buf, int offset, int length)
    {
        if (frameNumber > size)
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
    public boolean read(int frameNumber, byte[] buf, int offset, int length)
    {
        if (frameNumber > size)
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

    // allocate a new frame 
    public int newFrame()
    {
        if (frames.isEmpty())
        {
            return size++;
        }
        Integer result = frames.removeFirst();
        return result;
    }

    // allocate a new SwapPage when Memory page first time swap out 
    public SwapPage newSwapPage(MemoryPage memoryPage)
    {
        SwapPage swapPage = swapPageTable.get(memoryPage.processId ^ memoryPage.entry.vpn);
        if (swapPage == null)
        {
            swapPage = new SwapPage(memoryPage, newFrame());
            swapPageTable.put(memoryPage.processId ^ memoryPage.entry.vpn, swapPage);
        }
        return swapPage;
    }

    // return the SwapPage referenced by pid and vpn in disk 
    public SwapPage getSwapPage(int pid, int vpn)
    {
        return swapPageTable.get(pid ^ vpn);
    }

    // delete a SwapPage 
    public boolean deleteSwapPage(int pid, int vpn)
    {
        SwapPage swapPage = getSwapPage(pid, vpn);
        if (swapPage == null)
        {
            return false;
        }
        frames.add(swapPage.frameNumber);
        return true;
    }

    // return open file
    public OpenFile getSwapFile()
    {
        return file;
    }

}
