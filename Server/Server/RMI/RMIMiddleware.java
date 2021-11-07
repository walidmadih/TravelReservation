package Server.RMI;

import Server.Interface.IResourceManager;
import Server.Common.Customer;
import Server.Common.RMHashMap;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.concurrent.*;

public class RMIMiddleware implements IResourceManager{

    private String server_name = "Middleware";
    private IResourceManager manager_Flights = null;
    private IResourceManager manager_Cars = null;
    private IResourceManager manager_Rooms = null;

    public static void main(String args[]){
        String host1 = args[0];
        String host2 = args[1];
        String host3 = args[2];
        int port = 2034;
        String s_rmiPrefix = "group_34_";
        String server_name = "Middleware";

        if (args.length > 3)
        {
            port = Integer.parseInt(args[3]);
        }

        // Set the security policy
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            // Create a new Server object
            RMIMiddleware server = new RMIMiddleware();
            server.connectResources(host1,"Flights", port);
            server.connectResources(host2,"Cars", port);
            server.connectResources(host3,"Rooms", port);
            // Dynamically generate the stub (client proxy)
            IResourceManager middleware = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(port);
            }
            final Registry registry = l_registry;
            registry.rebind(s_rmiPrefix + server_name, middleware);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + server_name);
                        System.out.println("'" + server_name + "' resource manager unbound");
                    }
                    catch(Exception e) {
                        System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + server_name + "' resource manager server ready and bound to '" + s_rmiPrefix + server_name + "'");
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void connectResources(String host, String name, int port){
        String s_rmiPrefix = "group_34_";
        try{
            boolean first = true;
            while(true) {
                try{
                    Registry registry = LocateRegistry.getRegistry(host, port);
                    if(name.equals("Flights")) {
                        manager_Flights = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    }
                    else if(name.equals("Cars")){
                        manager_Cars = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    }
                    else{
                        manager_Rooms = (IResourceManager) registry.lookup(s_rmiPrefix + name);
                    }
                    System.out.println("Connected to '" + name + "' server [" + host + ":" + port + "/" + s_rmiPrefix + name + "]");
                    break;
                }
                catch (NotBoundException|RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + name + "' server [" + host + ":" + port + "/" + s_rmiPrefix + name + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        }
        catch(Exception e){
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException{
        final Callable<Boolean> addFlight = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return manager_Flights.addFlight(id,flightNum,flightSeats,flightPrice);
            }
        };
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Boolean> future = executor.submit(addFlight);
        executor.shutdown(); // This does not cancel the already-scheduled task.

        try {
            return future.get(20, TimeUnit.SECONDS);
        }
        catch (InterruptedException ie) {
            /* Handle the interruption. Or ignore it. */
        }
        catch (ExecutionException ee) {
            /* Handle the error. Or ignore it. */

        } catch (TimeoutException e) {
//            DeadlockException exception = new DeadlockException(id,"Abort transaction "+id);
              System.out.println("TonyWanghaha");
        }
        return false;
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException{
        return manager_Cars.addCars(id, location, numCars, price);
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException{
        return manager_Rooms.addRooms(id, location, numRooms, price);
    }

    public int newCustomer(int id) throws RemoteException{
        int cid = manager_Flights.newCustomer(id);
        manager_Cars.newCustomer(id, cid);
        manager_Rooms.newCustomer(id, cid);
        return cid;
    }

    public boolean newCustomer(int id, int cid) throws RemoteException{
        return manager_Flights.newCustomer(id, cid) &&
                manager_Rooms.newCustomer(id, cid) &&
                manager_Cars.newCustomer(id, cid);
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException{
        return manager_Flights.deleteFlight(id, flightNum);
    }

    public boolean deleteCars(int id, String location) throws RemoteException{
        return manager_Cars.deleteCars(id, location);
    }

    public boolean deleteRooms(int id, String location) throws RemoteException{
        return manager_Rooms.deleteRooms(id, location);
    }

    public boolean deleteCustomer(int id, int customerID) throws RemoteException{
        return manager_Flights.deleteCustomer(id, customerID) &&
                manager_Cars.deleteCustomer(id, customerID) &&
                manager_Rooms.deleteCustomer(id, customerID);
    }

    public int queryFlight(int id, int flightNumber) throws RemoteException{
        return manager_Flights.queryFlight(id, flightNumber);
    }

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) throws RemoteException{
        return manager_Cars.queryCars(id, location);
    }

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) throws RemoteException{
        return manager_Rooms.queryRooms(id, location);
    }

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) throws RemoteException{
        
        //Retrieve reservations from all 3 servers
        RMHashMap flightReservations = manager_Flights.queryCustomerReservations(id, customerID);
        RMHashMap carReservations = manager_Cars.queryCustomerReservations(id, customerID);
        RMHashMap roomReservations = manager_Rooms.queryCustomerReservations(id, customerID);

        //Combine all reservations into a single hashmap
        flightReservations.putAll(carReservations);
        flightReservations.putAll(roomReservations);

        //Create a temporary instance of customer to print the bill.
        Customer cust = new Customer(customerID, flightReservations);
        return cust.getBill();
    }

    /**
     * This method is not used in the middleware, only at the resource server level, so no implementation is needed.
     *
     * @return A map of the customer's reservations
     */
    public RMHashMap queryCustomerReservations(int id, int customerID) throws RemoteException{
        return null;
    }

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException{
        return manager_Flights.queryFlightPrice(id, flightNumber);
    }

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) throws RemoteException{
        return manager_Cars.queryCarsPrice(id, location);
    }

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) throws RemoteException{
        return manager_Rooms.queryRoomsPrice(id, location);
    }

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException{
        return manager_Flights.reserveFlight(id, customerID, flightNumber);
    }

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException{
        return manager_Cars.reserveCar(id, customerID, location);
    }

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException{
        return manager_Rooms.reserveRoom(id, customerID, location);
    }

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException{
        
        boolean somethingReserved = false;
        for (String flightNum : flightNumbers) {
            if (manager_Flights.reserveFlight(id, customerID, Integer.parseInt(flightNum)))
                somethingReserved = true;
        }

        if (car) {
            if (manager_Cars.reserveCar(id, customerID, location))
                somethingReserved = true;
        }

        if (room) {
            if (manager_Rooms.reserveRoom(id, customerID, location))
                somethingReserved = true;
        }

        return somethingReserved;
    }

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName() throws RemoteException{
        return server_name;
    }


}
