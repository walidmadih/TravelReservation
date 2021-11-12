package Client.DataGatherer;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;

import javax.sound.sampled.SourceDataLine;

import Client.RMIClient;
import Server.Interface.DataPoint;
import Server.Interface.LayerTypes;
import Server.Interface.TransactionTimer;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAbortedException;

import java.util.*;

public class TestClient extends RMIClient implements Runnable
{
    private String aHost;
    private int aPort;
    private String aGroupName;
    private String aServerName;
    // One transaciton will be executed each interval (milliseconds)
    private int aTransactionInterval = 500;
    // transactionTime belongs to [aTransactionInterval - aTransactionIntervalVariation, aTransactionInterval + aTransactionIntervalVariation]
    private int aTransactionIntervalVariation = 200;

    private boolean randomTransactions = false;

    public TestClient(String pHost, int pPort, String pServerName, String pGroupName){
        this(pHost, pPort, pServerName, pGroupName, 500);
    }
    public TestClient(String pHost, int pPort, String pServerName, String pGroupName, int pTransactionInterval){
        this(pHost, pPort, pServerName, pGroupName, pTransactionInterval, (pTransactionInterval / 2));
    }
    public TestClient(String pHost, int pPort, String pServerName, String pGroupName, int pTransactionInterval, int pTransactionIntervalVariation){
        aHost = pHost;
        aPort = pPort;
        aServerName = pServerName;
        aGroupName = pGroupName;
        aTransactionInterval = pTransactionInterval;
        aTransactionIntervalVariation = pTransactionIntervalVariation;
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
            System.out.println(String.format("New client connected to %s %d %s %s will be making a transaction every %d milliseconds with a variation of %d",
                aHost, aPort, aServerName, aGroupName, aTransactionInterval, aTransactionIntervalVariation));


            Random rand = new Random();
            startTime = System.currentTimeMillis();
            
            while(true){
                //Deciding time before next transaction
                int upper = aTransactionInterval + aTransactionIntervalVariation;
                int lower = aTransactionInterval - aTransactionIntervalVariation;
                int targetTime = rand.nextInt(upper) + lower;

                Transaction transaction = new Transaction(OperationGenerator.generateRandomOperations(), this);
                try{
                    transaction.start();
                }catch(InvalidTransactionException e){
                    transaction.abort(this);
                }catch(TransactionAbortedException e){
                    transaction.abort(this);
                }

                System.out.println(String.format("Transaction XID: %d\t\tOperation Count: %d\t\tTransaction Time: %d\t\tCOMPLETED", transaction.getXid(), transaction.getTotalCount(), transaction.getTransactionTime()));
                long sleepTime = Math.max(targetTime - (transaction.getTransactionTime()), 0);

                try{
                    Thread.sleep(sleepTime);
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