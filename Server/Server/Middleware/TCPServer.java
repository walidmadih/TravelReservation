package Server.Middleware;

import Client.RemoteMethod;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;

public class TCPServer {
    ServerSocket serverSocket;
    ObjectOutputStream out;
    ObjectInputStream in;

    public void start(int port){

        try {
            serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();

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
