package Client.DataGatherer;

import java.rmi.RemoteException;
import java.util.List;

import Client.Client;

public class DataTransaction implements ITransaction{
    private final Transaction aTransaction;
    private final DataPoint aDataPoint = new DataPoint();

    public DataTransaction(List<Operation> pOperations, Client pClient){
        aTransaction = new Transaction(pOperations, pClient);
    }

    public DataTransaction(Transaction pTransaction){
        aTransaction = pTransaction;
    }

    public int getTotalCount(){
        return aTransaction.getTotalCount();
    }

    public int start() throws RemoteException{
        long startTime = System.currentTimeMillis();
        int xid = aTransaction.start();
        long endTime = System.currentTimeMillis();

        return xid;
    }
}