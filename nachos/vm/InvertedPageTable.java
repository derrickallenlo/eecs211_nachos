package nachos.vm;

import java.util.ArrayList;
import nachos.machine.Lib;
import nachos.threads.Semaphore;

public class InvertedPageTable
{
    private static ArrayList<Integer> pidArray;
    private static ArrayList<Integer> vpnArray;
    private static ArrayList<Integer> ppnArray;
    private static boolean initialized = false;
    private static Semaphore sem;

    private InvertedPageTable()
    {

    }

    public static boolean initialize()
    {
        if (initialized)
        {
            return false;
        }
        initialized = true;
        pidArray = new ArrayList<Integer>();
        vpnArray = new ArrayList<Integer>();
        ppnArray = new ArrayList<Integer>();
        sem = new Semaphore(1);

        return true;
    }

    public static boolean put(Integer pid, Integer vpn, Integer ppn)
    {
        int size = vpnArray.size();
        Lib.assertTrue(pidArray.size() == size && size == ppnArray.size());
        sem.P();

        for (Integer temp : ppnArray)
        {
            if (temp == ppn)
            {
                Lib.assertNotReached("Detecting duplicate use of ppn: " + ppn);
            }
        }

        for (int i = 0; i < size; i++)
        {
            if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
            {

                ppnArray.set(i, ppn);
                sem.V();
                return true;
            }

        }

        pidArray.add(pid);
        vpnArray.add(vpn);
        ppnArray.add(ppn);
        sem.V();
        return false;
    }

    public static Integer get(Integer pid, Integer vpn)
    {
        int size = vpnArray.size();
        Lib.assertTrue(pidArray.size() == size && size == ppnArray.size());
        sem.P();
        for (int i = 0; i < size; i++)
        {
            if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
            {
                sem.V();
                return ppnArray.get(i);
            }

        }
        sem.V();
        return null;
    }

    public static Integer remove(Integer pid, Integer vpn)
    {
        int size = vpnArray.size();
        Lib.assertTrue(pidArray.size() == size && size == ppnArray.size());
        sem.P();
        for (int i = 0; i < size; i++)
        {
            if ((pidArray.get(i) == pid) && (vpnArray.get(i) == vpn))
            {
                int temp = ppnArray.get(i);
                pidArray.remove(i);
                vpnArray.remove(i);
                ppnArray.remove(i);
                sem.V();
                return temp;
            }

        }
        sem.V();
        return null;
    }

    public static boolean isInitialized()
    {
        return initialized;
    }

    public static String getString()
    {
        StringBuilder sb = new StringBuilder();

        int size = vpnArray.size();
        for (int i = 0; i < size; i++)
        {
            sb.append("      **pid: ").append(pidArray.get(i)).
                    append(", vpn: ").append(vpnArray.get(i)).
                    append(", ppn: ").append(ppnArray.get(i)).
                    append("\n");

        }
        return sb.toString();
    }
}
