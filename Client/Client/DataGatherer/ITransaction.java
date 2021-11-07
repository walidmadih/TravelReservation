package Client.DataGatherer;

import java.rmi.RemoteException;

public interface ITransaction{
    
    public int getTotalCount();
    public int start()
    throws RemoteException;

}