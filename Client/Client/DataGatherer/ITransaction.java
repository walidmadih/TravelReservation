package Client.DataGatherer;

import Server.Interface.IResourceManager;

import java.rmi.RemoteException;

public interface ITransaction{
    
    public int getTotalCount();
    public int start()
    throws RemoteException, IResourceManager.TransactionAbortedException, IResourceManager.InvalidTransactionException;

}