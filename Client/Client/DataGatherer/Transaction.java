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
import Server.Interface.IResourceManager.TransactionAlreadyWaitingException;

public class Transaction{
    private final LinkedList<Operation> aOperations;
    private final Client aClient;
    private final int aSize;

    private long startTime;
    private long endTime;
    private long transactionTime = 0;

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

    private void executeAllOperations() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        for(Operation operation : aOperations){
            System.out.println(String.format("Executing:  %s ", operation.toString()));
            operation.executeOnClient(aClient);
        }
    }

    private boolean commit() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        return aClient.commitTransaction(xid);
    }

    public void abort() throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        aClient.transactionLayerTimer.cleanUp(xid);
        aClient.abortTransaction(xid);
    }

    public boolean start() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        try{
            xid = aClient.startTransaction();
            System.out.println(String.format("Retrieved XID: %d", xid));
            startTime = System.currentTimeMillis();
            endTime = startTime;
            for(Operation operation: aOperations){
                operation.setXid(xid);
            }
            executeAllOperations();
            endTime = System.currentTimeMillis();
            commit();
            return true;
        }
        catch (Exception e) {
            abort();
            return false;
        }
    }

    public long getTransactionTime(){
        transactionTime = (endTime - startTime);
        return transactionTime;
    }

    public int getXid(){
        return xid;
    }
}