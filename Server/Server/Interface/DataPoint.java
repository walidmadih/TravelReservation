package Server.Interface;

import java.io.Serializable;

public class DataPoint implements Serializable{
    private LayerTypes aLayerTypes;
    private int aTotalTime;
    private int aTotalCount;
    public DataPoint(LayerTypes pLayerTypes, int pTotalTime, int pTotalCount){
        aLayerTypes = pLayerTypes;
        aTotalTime = pTotalTime;
        aTotalCount = pTotalCount;
    }
    public LayerTypes getLayer(){
        return aLayerTypes;
    }
    public int getTotalTime(){
        return aTotalTime;
    }
    public int getTotalCount(){
        return aTotalCount;
    }
}