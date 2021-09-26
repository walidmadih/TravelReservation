package Server.Middleware;

import Server.Interface.Command;
import Server.Interface.RemoteMethod;
import java.io.Serializable;
import java.util.Vector;

public class TCPMiddleware extends TCPServer {

    private static int port = 5163;
    private static String name = "Middleware";
    private static ResourceServer flightServer;
    private static ResourceServer roomServer;
    private static ResourceServer carServer;

    public static void main(String args[]) {
        if (!(args.length == 3)) {
            throw new IllegalArgumentException("Invalid Argument Count");
        }

        connectServers(args);

        // Create the RMI server entry
        try {
            TCPMiddleware middleware = new TCPMiddleware();
            middleware.start(port);
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public TCPMiddleware() {
        super();
    }

    private static void connectServers(String[] addresses) {
        flightServer = new ResourceServer(addresses[0], port);
        roomServer = new ResourceServer(addresses[1], port);
        carServer = new ResourceServer(addresses[2], port);
    }

    @Override
    public Serializable addFlight(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable addCars(Command cmd, Vector<String> arguments) {
        return carServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable addRooms(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable newCustomer(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable newCustomerId(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable deleteFlight(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable deleteCars(Command cmd, Vector<String> arguments) {
        return carServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable deleteRooms(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable deleteCustomer(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable queryFlight(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryCars(Command cmd, Vector<String> arguments) {
        return carServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryRooms(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryCustomerInfo(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable queryFlightPrice(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryCarsPrice(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable queryRoomsPrice(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable reserveFlight(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable reserveCar(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable reserveRoom(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable bundle(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
