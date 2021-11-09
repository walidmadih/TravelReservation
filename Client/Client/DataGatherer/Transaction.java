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
    private boolean committed;
    private boolean aborted;

    public static Transaction getDummyTransaction(){
        Transaction dummy = new Transaction(new LinkedList<Operation>(), null);
        dummy.setXid(-1);
        return dummy;
    }
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

    public void setXid(int id){
        xid = id;
    }
    public void setCommitted(boolean bool){
        committed = bool;
    }

    public void setAborted(boolean bool){
        aborted = bool;
    }

    public boolean getCommitted(){
        return committed;
    }

    private void executeAllOperations() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        for(Operation operation : aOperations){
            if(aborted){
                break;
            }
            System.out.println(String.format("Executing:  %s ", operation.toString()));
            operation.executeOnClient(aClient);
        }
    }


    private void startOperation() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        Command cmd = Command.Start;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());

        Operation startOperation = new Operation(cmd, arguments);

        startOperation.executeOnClient(aClient);
    }

    private void commitOperation() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        Command cmd = Command.Commit;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());

        Operation commitOperation = new Operation(cmd, arguments);

        commitOperation.executeOnClient(aClient);
    }

    private void abortOperation() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        Command cmd = Command.Abort;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());

        Operation abortOperation = new Operation(cmd, arguments);

        abortOperation.executeOnClient(aClient);
    }

    public void abort() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        abortOperation();
    }

    public void start() throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
        startOperation();
        System.out.println(String.format("Retrieved XID: %d", xid));
        startTime = System.currentTimeMillis();
        endTime = startTime;
        for(Operation operation: aOperations){
            operation.setXid(xid);
        }
        executeAllOperations();
        endTime = System.currentTimeMillis();
        commitOperation();
        if(!committed){
            abortOperation();
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