package nachos.vm;

import java.util.Hashtable;

/**
 * A <tt>ReplacementAlgorithm</tt> 
 * Abstract class 
 * need to initiate with any Page replacement algorithm
 */
public abstract class ReplacementAlgorithm
{
    /* main memory, an array indexed by frame number, stores a page p. */
    //public int memory[];
    //public int used_frames;
    public int num_faults;

    /* hashmap maintains which page resides in which frame for quick reference.
     * key:page number, value:frame number; saves a linear scan to check if
     * a page exists in memory.
     */
    public Hashtable<Integer, Integer> page_map;

    public ReplacementAlgorithm(Hashtable newpage_map)
    {
        //memory = new int[num_frames];
        this.page_map = newpage_map;
       // used_frames = 0;
        //num_faults = 0;

        //for (int i = 0; i < num_frames; i++)
            //memory[i] = -1;
    }


    /* this method will be defined by page replacement algorithm */
    abstract int findSwappedPage();

}