package Server.Middleware;

import Server.Interface.Command;
import Server.Interface.RemoteMethod;
import Server.Common.*;
import java.io.Serializable;
import java.util.Vector;

public class TCPMiddleware extends TCPServer {

    private static int port = 5163;
    private static String name = "Middleware";
    private static ResourceServer flightServer;
    private static ResourceServer roomServer;
    private static ResourceServer carServer;
    private static ResourceManager customerServer;

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
        customerServer = new ResourceManager("Customers");
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
        return customerServer.newCustomer(cmd, arguments);
    }

    @Override
    public Serializable newCustomerId(Command cmd, Vector<String> arguments) {
        return customerServer.newCustomerId(cmd, arguments);
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
        int xid = ResourceManager.toInt(arguments.elementAt(1));
        int customerID = ResourceManager.toInt(arguments.elementAt(2));

        Vector<ReservedItem> reservedItems = customerServer.queryCustomerItems(xid, customerID);

        if (reservedItems != null) {

            for (ReservedItem item : reservedItems) {

                Command cmd2 = Command.fromString("cancelItem");
                Vector<String> args2 = new Vector<String>();
                args2.add("cancelItem");
                args2.add(arguments.elementAt(1));
                args2.add(item.getKey());
                args2.add(String.valueOf(item.getCount()));
    
                char itemType = item.getKey().charAt(0);
    
                switch (itemType) {
                    case 'f':
                        flightServer.sendRemoteMethod(new RemoteMethod(cmd2, args2));
                        break;
                    case 'c':
                        carServer.sendRemoteMethod(new RemoteMethod(cmd2, args2));
                        break;
                    case 'r':
                        roomServer.sendRemoteMethod(new RemoteMethod(cmd2, args2));
                        break;
                }
            }
        }

        return customerServer.deleteCustomer(xid, customerID);
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
        return customerServer.queryCustomerInfo(cmd, arguments);
    }

    @Override
    public Serializable queryFlightPrice(Command cmd, Vector<String> arguments) {
        return flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryCarsPrice(Command cmd, Vector<String> arguments) {
        return carServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }

    @Override
    public Serializable queryRoomsPrice(Command cmd, Vector<String> arguments) {
        return roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));
    }



    @Override
    public Serializable reserveFlight(Command cmd, Vector<String> arguments) {

        int xid = ResourceManager.toInt(arguments.elementAt(1));
        int customerID = ResourceManager.toInt(arguments.elementAt(2));
        String flightnum = arguments.elementAt(3);

        //If queryCustomerInfo returns an empty string, then the customer doesn't exist
        if (!customerServer.queryCustomerInfo(0, customerID).isEmpty()) {
            int price = (int) flightServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));

            //If price > 0, then the item is available to be reserved
            if (price > 0){
                customerServer.addReservationToCustomer(xid, customerID, Flight.getKey(ResourceManager.toInt(flightnum)), flightnum, price);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Serializable reserveCar(Command cmd, Vector<String> arguments) {
        int xid = ResourceManager.toInt(arguments.elementAt(1));
        int customerID = ResourceManager.toInt(arguments.elementAt(2));
        String location = arguments.elementAt(3);

        //If queryCustomerInfo returns an empty string, then the customer doesn't exist
        if (!customerServer.queryCustomerInfo(0, customerID).isEmpty()) {
            int price = (int) carServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));

            //If price > 0, then the item is available to be reserved
            if (price > 0){
                customerServer.addReservationToCustomer(xid, customerID, Car.getKey(location), location, price);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Serializable reserveRoom(Command cmd, Vector<String> arguments) {
        int xid = ResourceManager.toInt(arguments.elementAt(1));
        int customerID = ResourceManager.toInt(arguments.elementAt(2));
        String location = arguments.elementAt(3);

        //If queryCustomerInfo returns an empty string, then the customer doesn't exist
        if (!customerServer.queryCustomerInfo(0, customerID).isEmpty()) {
            int price = (int) roomServer.sendRemoteMethod(new RemoteMethod(cmd, arguments));

            //If price > 0, then the item is available to be reserved
            if (price > 0){
                customerServer.addReservationToCustomer(xid, customerID, Room.getKey(location), location, price);
            }
            return true;
        } else {
            return false;
        }
    }

    //The next three methods are unused in the middleware, they are only added so we can reuse the interface.
    @Override
    public Serializable cancelItem(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable bundle(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }
}
