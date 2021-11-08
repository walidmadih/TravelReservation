package Client.DataGatherer;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class DataGatherer{

    private static String aHost = "localhost";
	private static String aServerName = "Middleware";
	private static int aPort = 2034;
    // Desired total throughput per second
    private static int aThroughput = 2;
    // Desired number of clients
    private static int aClientCount = 1;

	private static String aGroupName= "group_34_";
	public static void main(String args[])
	{	

		if (args.length > 5)
		{
			System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		if (args.length > 0)
		{
			aHost = args[0];
		}
		if (args.length > 1)
		{
			aServerName = args[1];
		}
		if (args.length > 2)
		{
			aPort = Integer.parseInt(args[2]);
		}
        if (args.length > 3){
            aThroughput = Integer.parseInt(args[3]);
        }

        if (args.length > 4){
            aClientCount = Integer.parseInt(args[4]);
        }

        int clientTransactionInterval = (1000 * aClientCount / aThroughput);

		PrintStream initialStream = System.out;	
		// Prevent everything from writing to console, we will give DataClient a dedicated stream
		System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        ArrayList<TestClient> clients = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for(int i = 0; i < aClientCount; i++){
            clients.add(new TestClient(aHost, aPort, aServerName, aGroupName, clientTransactionInterval));
            threads.add(new Thread(clients.get(i)));
            threads.get(i).start();
        }
		DataClient dataClient = new DataClient(aHost, aPort, aServerName, aGroupName, clients);
		dataClient.setOut(initialStream);
		Thread dataThread = new Thread(dataClient);
		dataThread.run();

	}

}