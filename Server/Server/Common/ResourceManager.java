// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject.LockType;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class ResourceManager implements IResourceManager
{
	protected String m_name = "";
	protected RMHashMap m_data = new RMHashMap();
	private TransactionTimer timer = new TransactionTimer();
	private LockManager lManager = new LockManager();
	private HashMap<Integer, Vector<Snapshot>> abortInfo = new HashMap<Integer, Vector<Snapshot>>();
	public ResourceManager(String p_name)
	{
		m_name = p_name;
	}

	// Reads a data item
	protected RMItem readData(int xid, String key)
	{
		synchronized(m_data) {
			RMItem item = m_data.get(key);
			if (item != null) {
				return (RMItem)item.clone();
			}
			return null;
		}
	}

	// Writes a data item
	protected void writeData(int xid, String key, RMItem value)
	{
		synchronized(m_data) {
			m_data.put(key, value);
		}
	}

	// Remove the item out of storage
	protected void removeData(int xid, String key)
	{
		synchronized(m_data) {
			m_data.remove(key);
		}
	}

	// Deletes the encar item
	protected boolean deleteItem(int xid, String key) throws RemoteException
	{
		Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		// Check if there is such an item in the storage
		if (curObj == null)
		{
			Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
			return false;
		}
		else
		{
			if (curObj.getReserved() == 0)
			{
				removeData(xid, curObj.getKey());
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
				return true;
			}
			else
			{
				Trace.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
				return false;
			}
		}
	}

	// Query the number of available seats/rooms/cars
	protected int queryNum (int xid, String key) throws RemoteException
	{
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
	protected int queryPrice(int xid, String key) throws RemoteException
	{
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
		ReservableItem curObj = (ReservableItem)readData(xid, key);
		int value = 0; 
		if (curObj != null)
		{
			value = curObj.getPrice();
		}
		Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
		return value;        
	}

	// Reserve an item
	protected boolean reserveItem(int xid, int customerID, String key, String location) throws RemoteException
	{
		Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called" );        
		// Read customer object if it exists (and read lock it)
		Customer customer = (Customer)readData(xid, Customer.getKey(customerID));
		if (customer == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
			return false;
		} 

		// Check if the item is available
		ReservableItem item = (ReservableItem)readData(xid, key);
		if (item == null)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
			return false;
		}
		else if (item.getCount() == 0)
		{
			Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
			return false;
		}
		else
		{            
			customer.reserve(key, location, item.getPrice());        
			writeData(xid, customer.getKey(), customer);

			// Decrease the number of available items in the storage
			item.setCount(item.getCount() - 1);
			item.setReserved(item.getReserved() + 1);
			writeData(xid, item.getKey(), item);

			Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
			return true;
		}        
	}

	// Create a new flight, or add seats to existing flight
	// NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
	public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if (lManager.Lock(xid, Flight.getKey(flightNum), LockType.LOCK_WRITE)) {
				Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
				Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));
				takeSnapshot(xid, Flight.getKey(flightNum));
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
				timer.stop(xid);
				return true;
			} else {
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Create a new car location or add cars to an existing location
	// NOTE: if price <= 0 and the location already exists, it maintains its current price
	public boolean addCars(int xid, String location, int count, int price) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Car.getKey(location), LockType.LOCK_WRITE)) {
				Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
				Car curObj = (Car) readData(xid, Car.getKey(location));
				takeSnapshot(xid, Car.getKey(location));
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
				timer.stop(xid);
				return true;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Create a new room location or add rooms to an existing location
	// NOTE: if price <= 0 and the room location already exists, it maintains its current price
	public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Room.getKey(location), LockType.LOCK_WRITE)) {
				Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
				Room curObj = (Room) readData(xid, Room.getKey(location));
				takeSnapshot(xid, Room.getKey(location));
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
				timer.stop(xid);
				return true;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Deletes flight
	public boolean deleteFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Flight.getKey(flightNum), LockType.LOCK_WRITE)) {
				takeSnapshot(xid, Flight.getKey(flightNum));
				boolean return_val = deleteItem(xid, Flight.getKey(flightNum));
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Delete cars at a location
	public boolean deleteCars(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Car.getKey(location), LockType.LOCK_WRITE)) {
				takeSnapshot(xid, Car.getKey(location));
				boolean return_val = deleteItem(xid, Car.getKey(location));
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Delete rooms at a location
	public boolean deleteRooms(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Room.getKey(location), LockType.LOCK_WRITE)) {
				takeSnapshot(xid, Room.getKey(location));
				boolean return_val = deleteItem(xid, Room.getKey(location));
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns the number of empty seats in this flight
	public int queryFlight(int xid, int flightNum) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Flight.getKey(flightNum), LockType.LOCK_READ)) {
				int res = queryNum(xid, Flight.getKey(flightNum));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns the number of cars available at a location
	public int queryCars(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Car.getKey(location), LockType.LOCK_READ)) {
				int res = queryNum(xid, Car.getKey(location));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns the amount of rooms available at a location
	public int queryRooms(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Room.getKey(location), LockType.LOCK_READ)) {
				int res = queryNum(xid, Room.getKey(location));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns price of a seat in this flight
	public int queryFlightPrice(int xid, int flightNum) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Flight.getKey(flightNum), LockType.LOCK_READ)) {
				int res = queryPrice(xid, Flight.getKey(flightNum));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns price of cars at this location
	public int queryCarsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Car.getKey(location), LockType.LOCK_READ)) {
				int res = queryPrice(xid, Car.getKey(location));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Returns room price at this location
	public int queryRoomsPrice(int xid, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Room.getKey(location), LockType.LOCK_READ)) {
				int res = queryPrice(xid, Room.getKey(location));
				timer.stop(xid);
				return res;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	public RMHashMap queryCustomerReservations(int xid, int customerID) throws RemoteException,TransactionAbortedException
	{
		timer.start(xid);
		Trace.info("RM::queryCustomerReservations(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		if (customer == null) {
			Trace.warn("RM::queryCustomerReservations(" + xid + ", " + customerID + ") failed--customer doesn't exist");
			// NOTE: don't change this--WC counts on this value indicating a customer does not exist...
			timer.stop(xid);
			throw new TransactionAbortedException();
		} else {
			Trace.info("RM::queryCustomerReservations(" + xid + ", " + customerID + ") succeeded.");
			timer.stop(xid);
			RMHashMap res = customer.getReservations();
			timer.stop(xid);
			return res;
		}
	}

	//This method is not used in the individual resource servers, only at the middleware level, so no implementation is needed.
	public String queryCustomerInfo(int xid, int customerID) throws RemoteException
	{
		return null;
	}

	//Not used at the RM layer, only at the middleware
	public int newCustomer(int xid) throws RemoteException,TransactionAbortedException
	{
		// Generate a globally unique ID for the new customer
		int cid = Integer.parseInt(String.valueOf(xid) +
				String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
				String.valueOf(Math.round(Math.random() * 100 + 1)));
		
		Trace.info("RM::newCustomer(" + xid + ") called");
		Customer customer = new Customer(cid);
		writeData(xid, customer.getKey(), customer);
		Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
		return cid;
	}

	public boolean newCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException
	{
		timer.start(xid);
		Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
		Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
		takeSnapshot(xid, Customer.getKey(customerID));
		if (customer == null) {
			customer = new Customer(customerID);
			writeData(xid, customer.getKey(), customer);
			Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
			timer.stop(xid);
			return true;
		} else {
			Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	public boolean deleteCustomer(int xid, int customerID) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
			Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
			takeSnapshot(xid, Customer.getKey(customerID));
			if (customer == null) {
				Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
				timer.stop(xid);
				throw new TransactionAbortedException();
			} 
			else 
			{
				// Increase the reserved numbers of all reservable items which the customer reserved.
				RMHashMap reservations = customer.getReservations();
				for (String reservedKey : reservations.keySet()) {
					ReservedItem reserveditem = customer.getReservedItem(reservedKey);
					if (lManager.Lock(xid, reserveditem.getKey(), LockType.LOCK_WRITE))
					{
						Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
						ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
						takeSnapshot(xid, reserveditem.getKey());
						Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount() + " times");
						item.setReserved(item.getReserved() - reserveditem.getCount());
						item.setCount(item.getCount() + reserveditem.getCount());
						writeData(xid, item.getKey(), item);
					}
					else
					{
						System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
						timer.stop(xid);
						throw new TransactionAbortedException();
					}
				}

				// Remove the customer from the storage
				removeData(xid, customer.getKey());
				Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
				timer.stop(xid);
				return true;
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}


	// Adds flight reservation to this customer
	public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Flight.getKey(flightNum), LockType.LOCK_WRITE) ) {
				takeSnapshot(xid, Flight.getKey(flightNum));
				takeSnapshot(xid, Customer.getKey(customerID));
				boolean return_val = reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Adds car reservation to this customer
	public boolean reserveCar(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Car.getKey(location), LockType.LOCK_WRITE) ) {
				takeSnapshot(xid, Car.getKey(location));
				takeSnapshot(xid, Customer.getKey(customerID));
				boolean return_val = reserveItem(xid, customerID, Car.getKey(location), location);
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Adds room reservation to this customer
	public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException,TransactionAbortedException,TransactionAlreadyWaitingException
	{
		try {
			timer.start(xid);
			if(lManager.Lock(xid, Room.getKey(location), LockType.LOCK_WRITE) ) {
				takeSnapshot(xid, Room.getKey(location));
				takeSnapshot(xid, Customer.getKey(customerID));
				boolean return_val = reserveItem(xid, customerID, Room.getKey(location), location);
				timer.stop(xid);
				if(!return_val){
					throw new TransactionAbortedException();
				}
				return return_val;
			}
			else{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(xid);
				throw new TransactionAbortedException();
			}
		}
		catch(DeadlockException deadlock){
			timer.stop(xid);
			throw new TransactionAbortedException();
		}
	}

	// Reserve bundle 
	public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException
	{
		return false;
	}

	public String getName() throws RemoteException
	{
		return m_name;
	}

	public void requestCustomerLock(int id, int customerID, LockType lockType) throws RemoteException, TransactionAbortedException, TransactionAlreadyWaitingException
	{
		timer.start(id);
		try
		{
			if(!lManager.Lock(id, Customer.getKey(customerID), lockType))
			{
				System.out.println("The input arguments for the lock is wrong, lock can't be granted. ");
				timer.stop(id);
				throw new TransactionAbortedException();
			}
		}
		catch (DeadlockException deadlock){
			timer.stop(id);
			throw new TransactionAbortedException();
		}
		timer.stop(id);
	}

	private void takeSnapshot(int xid, String key)
	{
		synchronized (abortInfo) {
			RMItem item = readData(xid, key);
			if (abortInfo.containsKey(xid)) {
				Vector<Snapshot> vect = abortInfo.get(xid);
				Snapshot snap = new Snapshot(key, item);
				if (!vect.contains(snap))
					vect.add(snap);
			} else {
				Vector<Snapshot> vect = new Vector<Snapshot>();
				vect.add(new Snapshot(key, item));
				abortInfo.put(xid, vect);
			}
		}
	}

	//Only implemented in the middleware layer, will not be called in the RM layer
	public int start() throws RemoteException {
		return 0;
	}

	@Override
	public boolean commit(int transactionId) throws RemoteException {
		synchronized (abortInfo) {
			//Release all locks taken by this transaction
			lManager.UnlockAll(transactionId);
			//Clean the snapshots taken before writing
			abortInfo.remove(transactionId);
			timer.commit(transactionId);
			return true;
		}
	}

	@Override
	public void abort(int transactionId) throws RemoteException {
		
		Trace.info("RM::abort(" + transactionId + ") called");

		Vector<Snapshot> vect = abortInfo.get(transactionId);

		if (vect != null)
		{
			for (Snapshot snap : vect)
			{
				if (snap.getItem() == null)
					removeData(transactionId, snap.getItemKey());
				else
					writeData(transactionId, snap.getItemKey(), snap.getItem());
			}
		}

		lManager.cleanupAbortedTransaction(transactionId);
		lManager.UnlockAll(transactionId);
		
		Trace.info("RM::abort(" + transactionId + ") completed successfully");
	}

	@Override
	public boolean shutdown() throws RemoteException {
		System.exit(0);
		return true;
	}

	@Override
	public Vector<DataPoint> queryTransactionResponseTime(int id, Vector<DataPoint> dataPoints) throws RemoteException {
		dataPoints.add(timer.getDataPoint(LayerTypes.DATABASE));
		return dataPoints;
	}
}
 
