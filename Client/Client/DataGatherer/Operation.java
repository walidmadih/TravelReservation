package Client.DataGatherer;

import java.rmi.RemoteException;
import java.util.Vector;

import Client.Client;
import Client.Command;
import Server.Interface.TransactionTimer;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAbortedException;
import Server.Interface.IResourceManager.TransactionAlreadyWaitingException;
public class Operation{

    private Command aCommand;
    private Vector<String> aArguments;
    private boolean xidSet = false;

    public Operation(Command pCommand, Vector<String> pArguments){
        aCommand = pCommand;
        aArguments = new Vector<String>(pArguments);
    }

    public void executeOnClient(Client pClient, Transaction callingTransaction) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        pClient.execute(aCommand, aArguments, callingTransaction);
    }

    public void setXid(int xid){
        if (xidSet)
            aArguments.set(1, String.valueOf(xid));
        else
            aArguments.insertElementAt(String.valueOf(xid), 1);
        xidSet = true;
    }

    public Operation copy(){
        return new Operation(this.aCommand, this.aArguments);
    }

    @Override
    public String toString(){
        String args = "";
        for(String s: aArguments){
            args += s + ",";
        }
        args = args.substring(0, args.length() - 1);
        return String.format("Command: %s\t\tArguments:%s", aCommand.name(), args);
    }
    
}