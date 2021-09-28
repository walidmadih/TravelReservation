package Server.Middleware;

import Server.Interface.Command;
import Server.Interface.IResourceManager;
import Server.Interface.RemoteMethod;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public abstract class TCPServer implements IResourceManager{

    ServerSocket serverSocket;

    private class TCPConnection extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        TCPConnection(Socket socket) {
            System.out.println("A connection with a client has been initialized.");
            clientSocket = socket;

            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                System.out.printf("Error setting up I/O streams.");
                System.out.println("The connection with the client has failed.");
                e.printStackTrace();
            }
            System.out.println("The connection with the client has been successfully established.");
        }

        public void run() {
            try {
                while (true) {
                    out.writeObject(executeRemoteMethod(in.readObject()));
                }
            } catch (Exception e) {
                System.out.println("Error While executing remote method.");
                e.printStackTrace();
            }
        }

        private Serializable executeRemoteMethod(Object o) {
            if (o instanceof RemoteMethod) {
                return remoteHandler((RemoteMethod) o);
            }
            System.out.println("ERROR object is not a RemoteMethod object");
            return false;
        }

    }

    // TCPServer Methods
    public void start(int port) {
        try {

            System.out.println("Creating TCP Server on port: " + port + ".");
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server on port: " + port + " has been created. Listening for client connections.");

        } catch (IOException e) {
            System.out.println("Could not create the server.");
            e.printStackTrace();
        }
        try {
            while (true) {
                new TCPConnection(serverSocket.accept()).run();
            }
        } catch (IOException e) {
            System.out.println("Failed to accept client connection.");
        }
    }


    protected Serializable remoteHandler(RemoteMethod remoteMethod) {
        Command cmd = remoteMethod.getCommand();
        Vector<String> arguments = remoteMethod.getArguments();
        Serializable response = null;
        switch (cmd) {
            case AddFlight: {
                response = addFlight(cmd, arguments);
                break;
            }
            case AddCars: {
                response = addCars(cmd, arguments);
                break;
            }
            case AddRooms: {
                response = addRooms(cmd, arguments);
                break;
            }
            case AddCustomer: {
                response = newCustomer(cmd, arguments);
                break;
            }
            case AddCustomerID: {
                response = newCustomerId(cmd, arguments);
                break;
            }
            case DeleteFlight: {
                response = deleteFlight(cmd, arguments);
                break;
            }
            case DeleteCars: {
                response = deleteCars(cmd, arguments);
                break;
            }
            case DeleteRooms: {
                response = deleteRooms(cmd, arguments);
                break;
            }
            case DeleteCustomer: {
                response = deleteCustomer(cmd, arguments);
                break;
            }
            case QueryFlight: {
                response = queryFlight(cmd, arguments);
                break;
            }
            case QueryCars: {
                response = queryCars(cmd, arguments);
                break;
            }
            case QueryRooms: {
                response = queryRooms(cmd, arguments);
                break;
            }
            case QueryCustomer: {
                response = queryCustomerInfo(cmd, arguments);
                break;
            }
            case QueryFlightPrice: {
                response = queryFlightPrice(cmd, arguments);
                break;
            }
            case QueryCarsPrice: {
                response = queryCarsPrice(cmd, arguments);
                break;
            }
            case QueryRoomsPrice: {
                response = queryRoomsPrice(cmd, arguments);
                break;
            }
            case ReserveFlight: {
                response = reserveFlight(cmd, arguments);
                break;
            }
            case ReserveCar: {
                response = reserveCar(cmd, arguments);
                break;
            }
            case ReserveRoom: {
                response = reserveRoom(cmd, arguments);
                break;
            }
            case CancelItem: {
                response = cancelItem(cmd, arguments);
                break;
            }
            case Bundle: {
                response = bundle(cmd, arguments);
                break;
            }
        }
        return response;
    }


}
