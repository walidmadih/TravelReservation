package Server.Middleware;

import Server.Common.RemoteMethod;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    ServerSocket serverSocket;

    public void start(int port){

        try {

            System.out.println("Creating TCP Server on port: " + port + ".");
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server on port: " + port + " has been created. Listening for client connections.");

        }catch (IOException e){
            System.out.println("Could not create the server.");
            e.printStackTrace();
        }
        try {
            while (true) {
                new TCPConnection(serverSocket.accept()).run();
            }
        }catch (IOException e){
            System.out.println("Failed to accept client connection.");
        }
    }

    private class TCPConnection extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        TCPConnection(Socket socket){
            System.out.println("A connection with a client has been initialized.");
            clientSocket = socket;

            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e){
                System.out.printf("Error setting up I/O streams.");
                System.out.println("The connection with the client has failed.");
                e.printStackTrace();
            }
            System.out.println("The connection with the client has been successfully established.");
        }

        public void run(){
            try {
                while (true) {
                    out.writeObject(executeRemoteMethod(in.readObject()));
                }
            }catch (Exception e){
                System.out.println("Error While executing remote method.");
                e.printStackTrace();
            }
        }

        private Serializable executeRemoteMethod(Object o){
            if (o instanceof RemoteMethod) {
                System.out.println("It is a RemoteMethod object");
                RemoteMethod remoteMethod = (RemoteMethod) o;
                System.out.println(remoteMethod.toString());
                return true;
            }
            System.out.println("ERROR object is not a RemoteMethod object");
            return false;
        }
    }


}
