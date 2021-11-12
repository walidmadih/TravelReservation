package Server.RMI;

import Server.Common.ResourceManager;
import Server.Interface.IResourceManager;
import Server.Interface.DataPoint;
import Server.Interface.LayerTypes;
import Server.Interface.TransactionTimer;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAlreadyWaitingException;
import Server.Common.Customer;
import Server.Common.RMHashMap;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionObject;
import Server.LockManager.TransactionLockObject.LockType;

import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.security.IdentityScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RMIMiddleware implements IResourceManager{

    private String server_name = "Middleware";
    private IResourceManager manager_Flights = null;
    private IResourceManager manager_Cars = null;
    private IResourceManager manager_Rooms = null;
    private TransactionTimer timer = new TransactionTimer();
    private AtomicInteger atomicID = new AtomicInteger(0);
    private AtomicInteger transactionID = new AtomicInteger(0);
    private AtomicInteger customerIDGenerator = new AtomicInteger(0);
    private HashMap<Integer, List<IResourceManager>> trans_active = new HashMap();
    private HashMap<Integer,Long> time_to_live = new HashMap();
    private HashSet<Integer> transactionsToNotify = new HashSet();
    private final Object lock = new Object();
    private long TTL = 60000;
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

    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)) {
                throw new InvalidTransactionException();
            }

            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, false, false);
            }

            timer.stop(id);

            return manager_Flights.addFlight(id, flightNum, flightSeats, flightPrice);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, true, false);
            }

            timer.stop(id);

            return manager_Cars.addCars(id, location, numCars, price);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, false, true);
            }

            timer.stop(id);

            return manager_Rooms.addRooms(id, location, numRooms, price);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public int newCustomer(int id) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);
                addRMToActiveTransaction(id, true, true, true);
            }
            
		    // Generate a globally unique ID for the new customer
		    int cid = customerIDGenerator.getAndIncrement();

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, cid, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, cid, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, cid, LockType.LOCK_WRITE);

            manager_Flights.newCustomer(id, cid);
            manager_Cars.newCustomer(id, cid);
            manager_Rooms.newCustomer(id, cid);

            return cid;
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean newCustomer(int id, int cid) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, cid, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, cid, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, cid, LockType.LOCK_WRITE);
            
            return manager_Flights.newCustomer(id, cid) &&
                manager_Rooms.newCustomer(id, cid) &&
                manager_Cars.newCustomer(id, cid);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean deleteFlight(int id, int flightNum) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, false, false);
            }

            timer.stop(id);

            return manager_Flights.deleteFlight(id, flightNum);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean deleteCars(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, true, false);
            }

            timer.stop(id);

            return manager_Cars.deleteCars(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean deleteRooms(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, false, true);
            }

            timer.stop(id);

            return manager_Rooms.deleteRooms(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public boolean deleteCustomer(int id, int customerID) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);

            return manager_Flights.deleteCustomer(id, customerID) &&
                manager_Cars.deleteCustomer(id, customerID) &&
                manager_Rooms.deleteCustomer(id, customerID);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public int queryFlight(int id, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, false, false);
            }

            timer.stop(id);

            return manager_Flights.queryFlight(id, flightNumber);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public int queryCars(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);

            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, true, false);
            }

            timer.stop(id);

            return manager_Cars.queryCars(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public int queryRooms(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, false, true);
            }

            timer.stop(id);

            return manager_Rooms.queryRooms(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public String queryCustomerInfo(int id, int customerID) throws RemoteException,InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException {
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);
                //Retrieve reservations from all 3 servers
                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_READ);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_READ);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_READ);

            RMHashMap flightReservations = manager_Flights.queryCustomerReservations(id, customerID);
            RMHashMap carReservations = manager_Cars.queryCustomerReservations(id, customerID);
            RMHashMap roomReservations = manager_Rooms.queryCustomerReservations(id, customerID);

            timer.start(id);
            //Combine all reservations into a single hashmap
            flightReservations.putAll(carReservations);
            flightReservations.putAll(roomReservations);

            //Create a temporary instance of customer to print the bill.
            Customer cust = new Customer(customerID, flightReservations);
            String bill = cust.getBill();
            timer.stop(id);
            return bill;
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * This method is not used in the middleware, only at the resource server level, so no implementation is needed.
     *
     * @return A map of the customer's reservations
     */
    public RMHashMap queryCustomerReservations(int id, int customerID) throws RemoteException, TransactionAbortedException, InvalidTransactionException, TransactionAlreadyWaitingException{
        return null;
    }

    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException, TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, false, false);
            }

            timer.stop(id);

            return manager_Flights.queryFlightPrice(id, flightNumber);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public int queryCarsPrice(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException, TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);

            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, true, false);
            }

            timer.stop(id);

            return manager_Cars.queryCarsPrice(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public int queryRoomsPrice(int id, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, false, false, true);
            }

            timer.stop(id);

            return manager_Rooms.queryRoomsPrice(id, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);

            return manager_Flights.reserveFlight(id, customerID, flightNumber);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException,InvalidTransactionException,TransactionAbortedException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);


                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);

            return manager_Cars.reserveCar(id, customerID, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }


    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException{
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if(!trans_active.containsKey(id)){
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, true, true);
            }
            
            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);

            return manager_Rooms.reserveRoom(id, customerID, location);
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException,TransactionAbortedException,InvalidTransactionException,TransactionAlreadyWaitingException {
        try {
            timer.start(id);
            long time = System.currentTimeMillis();
            checkTTL(time, id);
            if (!trans_active.containsKey(id)) {
                throw new InvalidTransactionException();
            }
            synchronized (lock) {
                time_to_live.replace(id, time);

                addRMToActiveTransaction(id, true, true, true);
            }

            timer.stop(id);

            manager_Flights.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Cars.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);
            manager_Rooms.requestCustomerLock(id, customerID, LockType.LOCK_WRITE);

            timer.start(id);

            boolean somethingReserved = false;
            for (String flightNum : flightNumbers) {
                timer.stop(id);
                if (manager_Flights.reserveFlight(id, customerID, Integer.parseInt(flightNum)))
                    somethingReserved = true;
                timer.start(id);
            }

            if (car) {
                timer.stop(id);
                if (manager_Cars.reserveCar(id, customerID, location))
                    somethingReserved = true;
                timer.start(id);
            }

            if (room) {
                timer.stop(id);
                if (manager_Rooms.reserveRoom(id, customerID, location))
                    somethingReserved = true;
                timer.start(id);
            }

            timer.stop(id);
            return somethingReserved;
        }
        catch(TransactionAbortedException e){
            throw handleTransactionAbort(id);
        }
    }

    public TransactionAbortedException handleTransactionAbort(int id) throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        timer.cleanUp(id);
        List<IResourceManager> RM_used = trans_active.get(id);

        if (RM_used != null){

            for(IResourceManager RM:RM_used){
                RM.abort(id);
            }
            synchronized (lock) {
                trans_active.remove(id);
                time_to_live.remove(id);
            }
        }
        return new TransactionAbortedException();
    }

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName() throws RemoteException{
        return server_name;
    }

    private void checkTTL(long currentTime, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException{
        synchronized(lock) {
            boolean throwException = false;

            if (transactionsToNotify.contains(xid)) {
                transactionsToNotify.remove(xid);
                throwException = true;
            }


            for (var entry : time_to_live.entrySet()) {
                long prevTime = entry.getValue();

                if (currentTime - prevTime > TTL) {
                    int id = entry.getKey();

                    if (id == xid)
                        throwException = true;
                    else
                        transactionsToNotify.add(id);

                    List<IResourceManager> RM_used = trans_active.get(id);

                    if (RM_used != null) {

                        for (IResourceManager RM : RM_used) {
                            RM.abort(id);
                        }
                        trans_active.remove(id);
                    }
                }
            }
            time_to_live.entrySet().removeIf(entry -> currentTime - entry.getValue() > TTL);

            if (throwException)
                throw new TransactionAbortedException();
        }
    }

    private void addRMToActiveTransaction(int id, boolean addFlightRM, boolean addCarsRM, boolean addRoomsRM)
    {
        if(addRoomsRM && !trans_active.get(id).contains(this.manager_Rooms)){
            trans_active.get(id).add(this.manager_Rooms);
        };
        if(addCarsRM && !trans_active.get(id).contains(this.manager_Cars)){
            trans_active.get(id).add(this.manager_Cars);
        };
        if(addFlightRM && !trans_active.get(id).contains(this.manager_Flights)){
            trans_active.get(id).add(this.manager_Flights);
        };
    }

    // Only used at the RM layer, so no implementation needed in the middleware.
    public void requestCustomerLock(int id, int customerID, LockType lockType) throws RemoteException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException
    {
        
    }

    @Override
    public int start() throws RemoteException {
        synchronized (lock) {
            int trans_ID = transactionID.getAndIncrement();
            List<IResourceManager> RM_used = new ArrayList<>();
            trans_active.put(trans_ID, RM_used);
            long time = System.currentTimeMillis();
            time_to_live.put(trans_ID, time);
            return trans_ID;
        }
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
        synchronized (lock) {

            try {
                long time = System.currentTimeMillis();
                checkTTL(time, transactionId);
            } catch (TransactionAbortedException e) {
                List<IResourceManager> RM_used = trans_active.get(transactionId);

                if (RM_used != null) {

                    for (IResourceManager RM : RM_used) {
                        RM.abort(transactionId);
                    }
                    trans_active.remove(transactionId);
                    time_to_live.remove(transactionId);
                }
                throw e;
            }

            List<IResourceManager> RM_used = trans_active.get(transactionId);

            if (RM_used == null)
                throw new InvalidTransactionException();

            for (IResourceManager RM : RM_used) {
                RM.commit(transactionId);
            }

            trans_active.remove(transactionId);
            time_to_live.remove(transactionId);

            timer.commit(transactionId);

            return true;
        }
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        timer.cleanUp(transactionId);
        synchronized (lock) {

            List<IResourceManager> RM_used = trans_active.get(transactionId);

            if (RM_used == null)
                throw new InvalidTransactionException();

            for (IResourceManager RM : RM_used) {
                RM.abort(transactionId);
            }

            trans_active.remove(transactionId);
            time_to_live.remove(transactionId);
        }
    }

    @Override
    public boolean shutdown() throws RemoteException {
        
        //Abort all ongoing transactions
        for (int xid : trans_active.keySet())
        {
            List<IResourceManager> RM_used = trans_active.get(xid);
    
            for(IResourceManager RM:RM_used){
                try{
                    RM.abort(xid);
                }
                catch (InvalidTransactionException e) {}
            }
        }

        (new GracefulExiter(manager_Flights)).start();
        (new GracefulExiter(manager_Cars)).start();
        (new GracefulExiter(manager_Rooms)).start();
        (new GracefulExiter(this)).start();
        return true;
    }

    @Override
    public Vector<DataPoint> queryTransactionResponseTime(int id, Vector<DataPoint> dataPoints) throws RemoteException {
        dataPoints.add(timer.getDataPoint(LayerTypes.MIDDLEWARE));
        Vector<DataPoint> response = manager_Rooms.queryTransactionResponseTime(id, dataPoints);
        response = manager_Cars.queryTransactionResponseTime(id, response);
        return manager_Flights.queryTransactionResponseTime(id, response);
    }

    public class GracefulExiter extends Thread {
        private IResourceManager manager;

        public GracefulExiter(IResourceManager manager)
        {
            this.manager = manager;
        }

        public void run()
        {
            if (manager instanceof RMIMiddleware){
                try{
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {}
                System.exit(0);
            }
            else{
                try {
                    manager.shutdown();
                }
                catch (RemoteException e) {}
            }
                
        }
    }
}
