package Server.Middleware;

import Server.Interface.RemoteMethod;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class ResourceServer {

    private String address;
    private int port;

    private class TempClient {

        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public TempClient() {
            connect();
        }

        private void connect() {
            try {
                clientSocket = new Socket(address, port);
                in = new ObjectInputStream(clientSocket.getInputStream());
                out =new ObjectOutputStream(clientSocket.getOutputStream());
            }catch (IOException e){
                try {
                    Thread.sleep(500);
                }catch (InterruptedException ie){
    
                }
                connect();
            }
        }

        public Serializable sendRemoteMethod(RemoteMethod remoteMethod){
            Serializable response = null;
            try {
                out.writeObject(remoteMethod);
                out.flush();
                response =  (Serializable) in.readObject();
            }catch (IOException e){
                e.printStackTrace();
                System.out.println("Could not communicate remote method to " + address);
            }catch (ClassNotFoundException e){
                e.printStackTrace();
                System.out.println("Could not find readable object " + address);
            }
            return response;
        }
    }

    public ResourceServer(String pAddress, int pPort) {
        address = pAddress;
        port = pPort;
    }

    public Serializable sendRemoteMethod(RemoteMethod remoteMethod){
        TempClient client = new TempClient();
        return client.sendRemoteMethod(remoteMethod);
    }
}
