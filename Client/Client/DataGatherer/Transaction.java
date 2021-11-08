package Client.DataGatherer;

import java.nio.file.attribute.AclEntry;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import Client.Client;
import Client.Command;

import Server.Interface.*;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAbortedException;

public class Transaction{
    private final LinkedList<Operation> aOperations;
    private final Client aClient;
    private final int aSize;

    private long startTime;
    private long endTime;
    private int transactionTime = 0;

    private int xid;

    public Transaction(List<Operation> pOperations, Client pClient){
        aOperations = new LinkedList<>();
        for(Operation op : pOperations){
            aOperations.add(op.copy());
        }
        aSize = aOperations.size();
        aClient = pClient;
    }

    public int getTotalCount(){
        return aSize;
    }

    private void executeAllOperations() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        for(Operation operation : aOperations){
            System.out.println(String.format("Executing:  %s ", operation.toString()));
            operation.executeOnClient(aClient);
        }
    }

    private boolean commit() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        return aClient.commitTransaction(xid);
    }

    public void abort(){
        //TODO Implement this
        aClient.transactionLayerTimer.cleanUp(xid);
    }

    public int start() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        xid = aClient.startTransaction();
        System.out.println(String.format("Retrieved XID: %d", xid));
        startTime = System.currentTimeMillis();
        endTime = startTime;
        for(Operation operation: aOperations){
            operation.setXid(xid);
        }
        executeAllOperations();
        endTime = System.currentTimeMillis();
        if(!commit()){
            abort();
        }
        return xid;
    }

    public int getTransactionTime(){
        transactionTime = (int) (endTime - startTime);
        return transactionTime;
    }

    public int getXid(){
        return xid;
    }
}