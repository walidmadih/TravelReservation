package Server.Interface;

import java.io.Serializable;
import java.util.EnumMap;


public class TransactionTimer implements Serializable{
    
    private final EnumMap<LayerTypes, Integer> aTimeTracker;
    private final EnumMap<LayerTypes, Long> aActiveTimers;

    public TransactionTimer(){
        aTimeTracker = new EnumMap<LayerTypes, Integer>(LayerTypes.class);
        for(LayerTypes layerType : LayerTypes.values()){
            aTimeTracker.put(layerType, 0);
        }
        aActiveTimers = new EnumMap<LayerTypes, Long>(LayerTypes.class);
    }

    public void start(LayerTypes pLayerType){
        aActiveTimers.put(pLayerType, System.currentTimeMillis());
    }

    public void stop(LayerTypes pLayerType){
        assert aActiveTimers.containsKey(pLayerType);
        long startTime = aActiveTimers.get(pLayerType);
        aActiveTimers.remove(pLayerType);
        long endTime = System.currentTimeMillis();
        aTimeTracker.put(pLayerType, (int) (aTimeTracker.get(pLayerType) + (endTime - startTime)) );
    }

}