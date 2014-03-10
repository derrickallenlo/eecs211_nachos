package nachos.vm;

import java.util.Hashtable;

/**
 * A <tt>SecondChanceReplacement</tt> 
 * The second-chance algorithm must maintain a pointer similar to the FIFO algorithm. 
 * In addition, it needs an array of u-bits, one for each frame.
 */


public class SecondChanceReplacement extends ReplacementAlgorithm
{
    private int tick;
    private int current_frame;//pointer point to the location to be replace
    private boolean u[];//u-bit true= 1 false=0
    /* add any fields necessary such as an array of use bits, the size of memory.length. */

    public SecondChanceReplacement(Hashtable newpage_map)
    {
        /* call memory constructor and init fields. */
        super(newpage_map);
        /*tick = 0;
        u=new boolean [num_frames];
        for (int i=0;i<num_frames; i++)
            u[i]=true;
        current_frame=0;*/
    }

    /**
  	 * find a page to be replace when page fault occurred 
  	 * @param non
  	 */
    public int findSwappedPage()
    {
//    	/*num_faults++;
//    	
//        if (used_frames < memory.length)
//        {
//            /* insert page in memory. */
//            memory[used_frames] = p;
//
//            /* set <page#, frame#) in page_map for quick lookup. */
//            page_map.put(p, used_frames);
//
//            used_frames++;
//        }
//        else
//        {
//            /* evict page pointed to by current_frame and if only if its u-bit is false, replace with new page, and increment. */
//            while (u[current_frame])//search for u-bit contain 0
//            {
//                u[current_frame]=false;
//
//                current_frame=++current_frame% memory.length;//advance next frame
//            }
//            u[current_frame]=true;
//            page_map.remove(memory[current_frame]); // remove existing page->frame mapping
//            page_map.put(p, current_frame); // set new page->frame mapping
//            memory[current_frame] = p; // update frame with the new page
//            //System.err.println(current_frame);//after the frames are full check the replacement
//            /* advance current frame. */
//            current_frame++;
//            current_frame %= memory.length;
//        }

        return 0;
    }
    
    /*returns the number of faults. */
    public int getNumberPageFault()
    {
        return num_faults;
    }
}