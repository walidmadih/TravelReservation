package Server.RMI;

import Server.Common.ResourceManager;
import Server.Interface.IResourceManager;
import Server.Common.Customer;
import Server.Common.RMHashMap;
import Server.LockManager.DeadlockException;

import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RMIMiddleware implements IResourceManager{

    private String server_name = "Middleware";
    private IResourceManager manager_Flights = null;
    private IResourceManager manager_Cars = null;
    private IResourceManager manager_Rooms = null;
    private AtomicInteger transactionID = new AtomicInteger(0);
    private HashMap<Integer, List<IResourceManager>> trans_active = new HashMap();
    private HashMap<Integer,Long> time_to_live = new HashMap();
    private long TTL = 10000;
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

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)) {
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val = manager_Flights.addFlight(id, flightNum, flightSeats, flightPrice);
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            }
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val = manager_Cars.addCars(id, location, numCars, price);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            }
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val = manager_Rooms.addRooms(id, location, numRooms, price);
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            }
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public int newCustomer(int id) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            int cid = manager_Flights.newCustomer(id);
            manager_Cars.newCustomer(id, cid);
            manager_Rooms.newCustomer(id, cid);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };
            return cid;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean newCustomer(int id, int cid) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val =  manager_Flights.newCustomer(id, cid) &&
                                  manager_Rooms.newCustomer(id, cid) &&
                                  manager_Cars.newCustomer(id, cid);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            boolean return_val = manager_Flights.deleteFlight(id, flightNum);

            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean deleteCars(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val = manager_Cars.deleteCars(id, location);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean deleteRooms(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            boolean return_val = manager_Rooms.deleteRooms(id, location);

            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            }

            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public boolean deleteCustomer(int id, int customerID) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            boolean return_val = manager_Flights.deleteCustomer(id, customerID) &&
                                 manager_Cars.deleteCustomer(id, customerID) &&
                                 manager_Rooms.deleteCustomer(id, customerID);

            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };

            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    public int queryFlight(int id, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }

            time_to_live.replace(id,time);

            int num = manager_Flights.queryFlight(id, flightNumber);

            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };

            return num;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);

            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }

            time_to_live.replace(id,time);

            int num = manager_Cars.queryCars(id, location);

            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };

            return num;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            int num = manager_Rooms.queryRooms(id, location);
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };

            return num;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) throws RemoteException,InvalidTransactionException, TransactionAbortedException {
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            //Retrieve reservations from all 3 servers
            RMHashMap flightReservations = manager_Flights.queryCustomerReservations(id, customerID);
            RMHashMap carReservations = manager_Cars.queryCustomerReservations(id, customerID);
            RMHashMap roomReservations = manager_Rooms.queryCustomerReservations(id, customerID);
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };
            //Combine all reservations into a single hashmap
            flightReservations.putAll(carReservations);
            flightReservations.putAll(roomReservations);

            //Create a temporary instance of customer to print the bill.
            Customer cust = new Customer(customerID, flightReservations);
            return cust.getBill();
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
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
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };

            int price = manager_Flights.queryFlightPrice(id, flightNumber);;
            return price;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);

            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }

            time_to_live.replace(id,time);

            int price = manager_Cars.queryCarsPrice(id, location);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };

            return price;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };
            int price = manager_Rooms.queryRoomsPrice(id, location);
            return price;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            boolean return_val = manager_Flights.reserveFlight(id, customerID, flightNumber);
            if(!trans_active.get(id).contains(this.manager_Flights)){
                trans_active.get(id).add(this.manager_Flights);
            };

            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);

            boolean return_val = manager_Cars.reserveCar(id, customerID, location);
            if(!trans_active.get(id).contains(this.manager_Cars)){
                trans_active.get(id).add(this.manager_Cars);
            };

            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException{
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id,time);
            boolean return_val = manager_Rooms.reserveRoom(id, customerID, location);
            if(!trans_active.get(id).contains(this.manager_Rooms)){
                trans_active.get(id).add(this.manager_Rooms);
            };

            return return_val;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException,InvalidTransactionException {
        try {
            long time = System.currentTimeMillis();
            checkTTL(time);
            if (!trans_active.containsKey(id)) {
                throw new InvalidTransactionException();
            }
            time_to_live.replace(id, time);

            boolean somethingReserved = false;
            for (String flightNum : flightNumbers) {
                if (manager_Flights.reserveFlight(id, customerID, Integer.parseInt(flightNum)))
                    somethingReserved = true;
                if (!trans_active.get(id).contains(this.manager_Flights)) {
                    trans_active.get(id).add(this.manager_Flights);
                }
            }

            if (car) {
                if (manager_Cars.reserveCar(id, customerID, location))
                    somethingReserved = true;
                if (!trans_active.get(id).contains(this.manager_Cars)) {
                    trans_active.get(id).add(this.manager_Cars);
                }
            }

            if (room) {
                if (manager_Rooms.reserveRoom(id, customerID, location))
                    somethingReserved = true;
                if (!trans_active.get(id).contains(this.manager_Rooms)) {
                    trans_active.get(id).add(this.manager_Rooms);
                }
            }

            return somethingReserved;
        }
        catch(TransactionAbortedException e){
            List<IResourceManager> RM_used = trans_active.get(id);
            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            trans_active.remove(id);
            time_to_live.remove(id);
            throw e;
        }
    }
    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName() throws RemoteException{
        return server_name;
    }

    public void checkTTL(long currentTime) throws RemoteException,InvalidTransactionException{
        for(var entry: time_to_live.entrySet()){
            long prev_time = entry.getValue();
            if((currentTime - prev_time) > TTL){
                int trans_ID = entry.getKey();
                List<IResourceManager> RM_used = trans_active.get(trans_ID);
                for (IResourceManager manager:RM_used){
                    manager.abort(trans_ID);
                }
                trans_active.remove(trans_ID);
            }
        }
        time_to_live.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > TTL);
    }

    @Override
    public int start() throws RemoteException {
        transactionID.getAndIncrement();
        int trans_ID = transactionID.intValue();
        List<IResourceManager> RM_used = new ArrayList<>();
        trans_active.put(trans_ID,RM_used);
        long time = System.currentTimeMillis();
        time_to_live.put(trans_ID,time);
        return trans_ID;
    }

    @Override
    public boolean commit(int transactionId)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean shutdown() throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }


}
