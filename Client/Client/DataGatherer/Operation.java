package Client.DataGatherer;

import java.rmi.RemoteException;
import java.util.Vector;

import Client.Client;
import Client.Command;
import Server.Interface.TransactionTimer;
import Server.Interface.IResourceManager.TransactionAbortedException;
import Server.Interface.IResourceManager.InvalidTransactionException;

public class Operation{

    private Command aCommand;
    private Vector<String> aArguments;

    public Operation(Command pCommand, Vector<String> pArguments){
        aCommand = pCommand;
        aArguments = new Vector<String>(pArguments);
    }

    public void executeOnClient(Client pClient) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        pClient.execute(aCommand, aArguments);
    }

    public void setXid(int xid){
        aArguments.insertElementAt(String.valueOf(xid), 1);
    }

    public void setTimer(TransactionTimer timer){
        
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