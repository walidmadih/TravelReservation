package Client.DataGatherer;

import java.io.PrintStream;
import java.util.ArrayList;

import javax.sound.sampled.SourceDataLine;

import Client.Client;
import Client.RMIClient;

public class DataClient extends RMIClient implements Runnable
{
    private String aHost;
    private int aPort;
    private String aGroupName;
    private String aServerName;

    public DataClient(String pHost, int pPort, String pServerName, String pGroupName, ArrayList<TestClient> pClients){
        aHost = pHost;
        aPort = pPort;
        aServerName = pServerName;
        aGroupName = pGroupName;
        clients = pClients;
    }

    @Override
    public void run() {

        // Set the security policy
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new SecurityManager());
		}

		// Get a reference to the RMIRegister
		try {
            connectServer(aHost, aPort, aServerName, aGroupName);

            while(true){
                // Deciding time before next transaction
                Transaction transaction = new Transaction(OperationGenerator.generateTimeDataTransaction(), this);
                transaction.start();
                System.out.println(String.format("Transaction XID: %d\t\tOperation Count: %d\t\tTransaction Time: %d\t\tCOMPLETED", transaction.getXid(), transaction.getTotalCount(), transaction.getTransactionTime()));
                
                try{
                    Thread.sleep(1000);
                } catch(InterruptedException e){

                }
            }
		} 
		catch (Exception e) {    
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
        
        
    }

}