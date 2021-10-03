// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.Middleware.TCPServer;

import java.util.*;
import java.io.*;

public class ResourceManager extends TCPServer {
    private static String m_name = "";
    private static RMHashMap m_data = new RMHashMap();
    private static int port = 5163;

    public ResourceManager(String p_name) {
        m_name = p_name;
    }

    public static void main(String args[]) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Invalid Argument Count");
        }

        m_name = args[0];

        if (args.length == 2) {
            port = Integer.parseInt(args[1]);
        }

        // Create the RMI server entry
        try {
            ResourceManager resourceManager = new ResourceManager(m_name);
            resourceManager.start(port);
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

    // Reads a data item
    protected RMItem readData(int xid, String key) {
        synchronized (m_data) {
            RMItem item = m_data.get(key);
            if (item != null) {
                return (RMItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    protected void writeData(int xid, String key, RMItem value) {
        synchronized (m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    protected void removeData(int xid, String key) {
        synchronized (m_data) {
            m_data.remove(key);
        }
    }

    // Deletes the encar item
    protected boolean deleteItem(int xid, String key) {
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(xid, curObj.getKey());
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Query the number of available seats/rooms/cars
    protected int queryNum(int xid, String key) {
        Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    // Query the price of an item
    protected int queryPrice(int xid, String key) {
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readData(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    // Reserve an item
    protected int reserveItem(int xid, String key, String location) {
        Trace.info("RM::reserveItem(" + xid + ", " + key + ", " + location + ") called");

        // Check if the item is available
        ReservableItem item = (ReservableItem) readData(xid, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + key + ", " + location + ") failed--item doesn't exist");
            return 0;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + xid + ", " + key + ", " + location + ") failed--No more items");
            return 0;
        } else {

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            writeData(xid, item.getKey(), item);

            Trace.info("RM::reserveItem(" + xid + ", " + key + ", " + location + ") succeeded");
            return item.getPrice();
        }
    }

    protected boolean cancelItem(int xid, String key, int numToCancel){

        Trace.info("RM::cancelItem(" + xid + ", " + key + ", " + numToCancel + ") called");

        Trace.info("RM::cancelItem(" + xid + ", " + key + ", " + numToCancel + ") has reserved " + key + " " + numToCancel + " times");
        ReservableItem item = (ReservableItem) readData(xid, key);

        if (item != null) {

            Trace.info("RM::cancelItem(" + xid + ", " + key + ", " + numToCancel + ") has reserved " + key + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount() + " times");
            item.setReserved(item.getReserved() - numToCancel);
            item.setCount(item.getCount() + numToCancel);
            writeData(xid, item.getKey(), item);
        }

        return true;
    }

    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) {
        Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
        Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            // Doesn't exist yet, add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        } else {
            // Add seats to existing flight and update the price if greater than zero
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
        }
        return true;
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) {
        Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Car curObj = (Car) readData(xid, Car.getKey(location));
        if (curObj == null) {
            // Car location doesn't exist yet, add it
            Car newObj = new Car(location, count, price);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing car location and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) {
        Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Room curObj = (Room) readData(xid, Room.getKey(location));
        if (curObj == null) {
            // Room location doesn't exist yet, add it
            Room newObj = new Room(location, count, price);
            writeData(xid, newObj.getKey(), newObj);
            Trace.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing object and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            writeData(xid, curObj.getKey(), curObj);
            Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) {
        return deleteItem(xid, Flight.getKey(flightNum));
    }

    // Delete cars at a location
    public boolean deleteCars(int xid, String location) {
        return deleteItem(xid, Car.getKey(location));
    }

    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) {
        return deleteItem(xid, Room.getKey(location));
    }

    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) {
        return queryNum(xid, Flight.getKey(flightNum));
    }

    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) {
        return queryNum(xid, Car.getKey(location));
    }

    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) {
        return queryNum(xid, Room.getKey(location));
    }

    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) {
        return queryPrice(xid, Flight.getKey(flightNum));
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) {
        return queryPrice(xid, Car.getKey(location));
    }

    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) {
        return queryPrice(xid, Room.getKey(location));
    }

    public String queryCustomerInfo(int xid, int customerID) {
        Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return "";
        } else {
            Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
            System.out.println(customer.getBill());
            return customer.getBill();
        }
    }

    public boolean doesCustomerExist(int xid, int customerID) {
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        return customer != null;
    }

    public Vector<ReservedItem> queryCustomerItems(int xid, int customerID) {

        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));

        if (customer == null) {
            return null;
        }

        RMHashMap reservations = customer.getReservations();
        Vector<ReservedItem> reservedItems = new Vector<ReservedItem>();

        for (String reservedKey : reservations.keySet()) {
            reservedItems.add(customer.getReservedItem(reservedKey));
        }

        return reservedItems;
    }

    public int newCustomer(int xid) {
        Trace.info("RM::newCustomer(" + xid + ") called");
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer customer = new Customer(cid);
        writeData(xid, customer.getKey(), customer);
        Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
        return cid;
    }

    public boolean newCustomer(int xid, int customerID) {
        Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            customer = new Customer(customerID);
            writeData(xid, customer.getKey(), customer);
            Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    public boolean addReservationToCustomer(int xid, int customerID, String key, String location, int itemPrice) {
        Trace.info("RM::addReservationToCustomer(" + xid + ", " + customerID + ", " + location + ", " + itemPrice + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));

        if (customer == null) {
            Trace.info("INFO: RM::addReservationToCustomer(" + xid + ", " + customerID + ", " + location + ", " + itemPrice + ") failed--customer does not exist");
            return false;
        } else {
            customer.reserve(key, location, itemPrice);
            writeData(xid, customer.getKey(), customer);

            Trace.info("INFO: RM::addReservationToCustomer(" + xid + ", " + customerID + ", " + location + ", " + itemPrice + ") succeeded.");
            return true;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Remove the customer from the storage
            removeData(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    // Adds flight reservation to this customer
    public int reserveFlight(int xid, int flightNum) {
        return reserveItem(xid, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Adds car reservation to this customer
    public int reserveCar(int xid, String location) {
        return reserveItem(xid, Car.getKey(location), location);
    }

    // Adds room reservation to this customer
    public int reserveRoom(int xid, String location) {
        return reserveItem(xid, Room.getKey(location), location);
    }

    // Parse data
    @Override
    public Serializable addFlight(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(5, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        int flightNum = toInt(arguments.elementAt(2));
        int flightSeats = toInt(arguments.elementAt(3));
        int flightPrice = toInt(arguments.elementAt(4));

        return addFlight(id, flightNum, flightSeats, flightPrice);
    }

    @Override
    public Serializable addCars(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(5, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);
        int numCars = toInt(arguments.elementAt(3));
        int price = toInt(arguments.elementAt(4));

        return addCars(id, location, numCars, price);
    }

    @Override
    public Serializable addRooms(Command cmd, Vector<String> arguments) {

        if (!checkArgumentsCount(5, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(2);
        int numRooms = toInt(arguments.elementAt(3));
        int price = toInt(arguments.elementAt(4));

        return addRooms(id, location, numRooms, price);
    }

    @Override
    public Serializable newCustomer(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(2, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));

        return newCustomer(id);
    }

    @Override
    public Serializable newCustomerId(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        return newCustomer(id, customerID);
    }

    @Override
    public Serializable deleteFlight(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            int flightNum = toInt(arguments.elementAt(2));

            return deleteFlight(id, flightNum);
    }

    @Override
    public Serializable deleteCars(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return deleteCars(id, location);
    }

    @Override
    public Serializable deleteRooms(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return deleteRooms(id, location);
    }

    @Override
    public Serializable deleteCustomer(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        return deleteCustomer(id, customerID);
    }

    @Override
    public Serializable queryFlight(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            int flightNum = toInt(arguments.elementAt(2));

            return queryFlight(id, flightNum);
    }

    @Override
    public Serializable queryCars(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}


            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return queryCars(id, location);
    }

    @Override
    public Serializable queryRooms(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return queryRooms(id, location);
    }

    @Override
    public Serializable queryCustomerInfo(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        int customerID = toInt(arguments.elementAt(2));

        return queryCustomerInfo(id, customerID);
    }

    @Override
    public Serializable queryFlightPrice(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}


            int id = toInt(arguments.elementAt(1));
            int flightNum = toInt(arguments.elementAt(2));

            return queryFlightPrice(id, flightNum);
    }

    @Override
    public Serializable queryCarsPrice(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return queryCarsPrice(id, location);
    }

    @Override
    public Serializable queryRoomsPrice(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(3, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            String location = arguments.elementAt(2);

            return queryRoomsPrice(id, location);
    }

    @Override
    public Serializable reserveFlight(Command cmd, Vector<String> arguments) {
            if (!checkArgumentsCount(4, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

            int id = toInt(arguments.elementAt(1));
            int flightNum = toInt(arguments.elementAt(3));

            return reserveFlight(id, flightNum);
    }

    @Override
    public Serializable reserveCar(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(4, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(3);

        return reserveCar(id, location);
    }

    @Override
    public Serializable reserveRoom(Command cmd, Vector<String> arguments) {
        if (!checkArgumentsCount(4, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        String location = arguments.elementAt(3);

        return reserveRoom(id, location);
    }

    @Override 
    public Serializable cancelItem(Command cmd, Vector<String> arguments){
        if (!checkArgumentsCount(4, arguments.size())){return new IllegalArgumentException("Invalid number of arguments.");}

        int id = toInt(arguments.elementAt(1));
        String key = arguments.elementAt(2);
        int numToCancel = toInt(arguments.elementAt(3));

        return cancelItem(id, key, numToCancel);
    }

    @Override
    public Serializable bundle(Command cmd, Vector<String> arguments) {
        return null;
    }

    public String getName() {
        return m_name;
    }

    public static boolean checkArgumentsCount(Integer expected, Integer actual) {
        if (expected != actual) {
            return false;
        }
        return true;
    }

    public static int toInt(String string) throws NumberFormatException {
        return (Integer.valueOf(string)).intValue();
    }

    public static boolean toBoolean(String string)// throws Exception
    {
        return (Boolean.valueOf(string)).booleanValue();
    }
}
