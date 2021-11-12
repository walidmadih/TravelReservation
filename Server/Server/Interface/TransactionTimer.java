package Server.Interface;

import java.io.Serializable;
import java.lang.Character.Subset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Action;


public class TransactionTimer{
    
    private final HashSet<Integer> aCommitedIds;
    private final HashMap<Integer, Integer> aTimeTracker;
    private final HashMap<Integer, Long> aActiveTimers;
    private final Object lock = new Object();

    public TransactionTimer(){
        synchronized(lock){
            aTimeTracker = new HashMap<Integer, Integer>();
            aActiveTimers = new HashMap<Integer, Long>();
            aCommitedIds = new HashSet<Integer>();
        }
    }

    public void start(int xid) throws RemoteException{
        synchronized(lock){
            aActiveTimers.put(xid, System.currentTimeMillis());
        }
    }

    public void stop(int xid){
        synchronized(lock){
            if(!aActiveTimers.containsKey(xid)){
                return;
            }
            long startTime = aActiveTimers.get(xid);
            aActiveTimers.remove(xid);
            long endTime = System.currentTimeMillis();
            int timeSpent = aTimeTracker.containsKey(xid) ? aTimeTracker.get(xid) : 0;
            aTimeTracker.put(xid, (int) ( timeSpent + (endTime - startTime)) );
        }
    }

    public Integer getXidTime(int xid){  
        return aTimeTracker.get(xid);
    }

    public void commit(int xid){
        synchronized(lock){
            if(aTimeTracker.containsKey(xid)){
                aCommitedIds.add(xid);
            }
        }
    }

    public void cleanUp(int xid){
        synchronized(lock){
            if(aTimeTracker.containsKey(xid)){
                aTimeTracker.remove(xid);
            }
            if(aActiveTimers.containsKey(xid)){
                aActiveTimers.remove(xid);
            }
        }
    }

    public DataPoint getDataPoint(LayerTypes pLayerType){
        int total = 0;
        int size = aCommitedIds.size();
        synchronized(lock){
            ArrayList<Integer> toRemove = new ArrayList<>();
            total = 0;
            size = aCommitedIds.size();
            Iterator<Integer> it = aCommitedIds.iterator();
            for(Integer id : aCommitedIds){
                try{
                    total += aTimeTracker.get(id);
                }catch(Exception e){

                }
                aTimeTracker.remove(id);
                toRemove.add(id);
            }
            for(Integer id : toRemove){
                aCommitedIds.remove(id);
            }
        }
        return new DataPoint(pLayerType, total, size);
    }

}