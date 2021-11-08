package Client.DataGatherer;

import java.nio.file.attribute.AclEntry;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import Client.Client;
import Client.Command;

import Server.Interface.*;

public class Transaction{
    private final LinkedList<Operation> aOperations;
    private final Client aClient;
    private final int aSize;

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

    private void executeAllOperations() throws RemoteException, IResourceManager.InvalidTransactionException, IResourceManager.TransactionAbortedException, IResourceManager.TransactionAlreadyWaitingException {
        for(Operation operation : aOperations){
            operation.executeOnClient(aClient);
        }
    }

    private boolean commit(){
        //TODO Implement this
        return true;
    }

    public void abort(){
        //TODO Implement this
    }

    public int start() throws RemoteException, IResourceManager.InvalidTransactionException, IResourceManager.TransactionAbortedException, IResourceManager.TransactionAlreadyWaitingException {
        int xid = aClient.startTransaction();
        //try{
        TransactionTimer timer = new TransactionTimer();
        for(Operation operation: aOperations){
            operation.setXid(xid);
        }
        executeAllOperations();
        if (commit()){

        } else {
            abort();
        }

        //} catch(TransactionAbortedException){

        //} catch(InvalidTransacitonException){

        //}
        return xid;
    }
}