package Server.Middleware;

import Server.Common.RemoteMethod;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    ServerSocket serverSocket;
    ObjectOutputStream out;
    ObjectInputStream in;

    public void start(int port){

        try {

            System.out.println("Creating TCP Server on port: " + port + ".");
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server on port: " + port + " has been created. Listening for client connections.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client found. Connected to Client");

            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());


        }catch (IOException e){
            System.out.println("Could not create the server.");
            e.printStackTrace();
        }

        while (true) {
            try {
                RemoteMethod remoteMethod = (RemoteMethod) in.readObject();
                System.out.println(remoteMethod.toString());
            } catch (IOException e) {
                e.printStackTrace();

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


}
