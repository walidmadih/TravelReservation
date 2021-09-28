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

            // Cancel the reservation for each of the customer's items on their respective server
            for (ReservedItem item : reservedItems) {

                Command cmd2 = Command.fromString("cancelItem");
                Vector<String> args2 = new Vector<String>();
                args2.add("cancelItem");
                args2.add(arguments.elementAt(1));
                args2.add(item.getKey());
                args2.add(String.valueOf(item.getCount()));
    
                //The item key always starts with either flight, room or car, so we can use the first character of the key to identify the item's type
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
                return true;
            }
        }
        return false;
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
                return true;
            }
        }
        return false;
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
                return true;
            }
        }
        return false;
    }

    //This method is unused in the middleware, it is only added so we can reuse the interface.
    @Override
    public Serializable cancelItem(Command cmd, Vector<String> arguments) {
        return null;
    }

    @Override
    public Serializable bundle(Command cmd, Vector<String> arguments) {
        
        int id = ResourceManager.toInt(arguments.elementAt(1));
        int customerID = ResourceManager.toInt(arguments.elementAt(2));
        Vector<String> flightNumbers = new Vector<String>();
        for (int i = 0; i < arguments.size() - 6; ++i)
        {
            flightNumbers.addElement(arguments.elementAt(3+i));
        }
        String location = arguments.elementAt(arguments.size()-3);
        boolean car = ResourceManager.toBoolean(arguments.elementAt(arguments.size()-2));
        boolean room = ResourceManager.toBoolean(arguments.elementAt(arguments.size()-1));

        boolean somethingReserved = false;

        for (String flightNum : flightNumbers)
        {
            Command cmd2 = Command.fromString("reserveFlight");
            Vector<String> args2 = new Vector<String>();
            args2.add("reserveFlight");
            args2.add(arguments.elementAt(1));
            args2.add(arguments.elementAt(2));
            args2.add(flightNum);

            if ((boolean) reserveFlight(cmd2, args2))
                somethingReserved = true;
        }

        //The arguments vector will be the same to reserve a car or a room
        Vector<String> args2 = new Vector<String>();
        args2.add(arguments.elementAt(1));
        args2.add(arguments.elementAt(2));
        args2.add(location);

        if (car) {
            Command cmd2 = Command.fromString("reserveCar");
            args2.insertElementAt("reserveCar", 0);

            if ((boolean) reserveCar(cmd2, args2))
                somethingReserved = true;

            //Remove the command from the start of the args vector, in case we also need to add a room
            args2.remove(0);
        }

        if (room) {
            Command cmd2 = Command.fromString("reserveRoom");
            args2.insertElementAt("reserveRoom", 0);

            if ((boolean) reserveRoom(cmd2, args2))
                somethingReserved = true;
        }

        return somethingReserved;
    }

    @Override
    public String getName() {
        return name;
    }
}
