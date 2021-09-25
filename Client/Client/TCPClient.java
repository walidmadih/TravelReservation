package Client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.ConnectException;

public class TCPClient extends Client {

    private static String s_serverHost = "localhost";
    private static int s_serverPort = 5163;
    private static String s_serverName = "Server";


    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            s_serverName = args[1];
        }
        if (args.length > 2) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        try {
            TCPClient client = new TCPClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TCPClient() {
        super();
    }

    public void connectServer() {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name) {
        try {
            boolean first = true;
            while (true) {
                try {
                    clientSocket = new Socket(s_serverHost, s_serverPort);
                    objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                    System.out.println("Connected to '" + name + "' TCP server [" + server + ":" + port + "/" + name + "]");
                    break;
                } catch (Exception e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' TCP server [" + server + ":" + port + "/" + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
