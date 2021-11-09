package Client;

import Server.Interface.*;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAbortedException;

import java.util.*;

import javax.xml.crypto.Data;

import Client.DataGatherer.TestClient;
import Client.DataGatherer.Transaction;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import Server.Interface.IResourceManager.TransactionAbortedException;
import Server.Interface.IResourceManager.InvalidTransactionException;
import Server.Interface.IResourceManager.TransactionAlreadyWaitingException;

public abstract class Client
{
	IResourceManager m_resourceManager = null;
	public TransactionTimer timer = new TransactionTimer();
	public TransactionTimer transactionLayerTimer = new TransactionTimer();
	protected ArrayList<TestClient> clients;
	private PrintStream out;

	public void setOut(PrintStream ps){
		out = ps;
	}

	public Client()
	{
		super();
	}

	public abstract void connectServer();

	public void start()
	{
		// Prepare for reading commands
		System.out.println();
		System.out.println("Location \"help\" for list of supported commands");

		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

		while (true)
		{
			// Read the next command
			String command = "";
			Vector<String> arguments = new Vector<String>();
			try {
				System.out.print((char)27 + "[32;1m\n>] " + (char)27 + "[0m");
				command = stdin.readLine().trim();
			}
			catch (IOException io) {
				System.err.println((char)27 + "[31;1mClient exception: " + (char)27 + "[0m" + io.getLocalizedMessage());
				io.printStackTrace();
				System.exit(1);
			}

			try {
				arguments = parse(command);
				Command cmd = Command.fromString((String)arguments.elementAt(0));
				try {
					execute(cmd, arguments);
				}
				catch (ConnectException e) {
					connectServer();
					execute(cmd, arguments);
				}
				catch (TransactionAbortedException e) {
					System.out.println("Transaction has been aborted.");
				}
				catch (InvalidTransactionException e) {
					System.out.println("The provided transaction ID is invalid.");
				}
				catch (TransactionAlreadyWaitingException e) {
					System.out.println("This transaction is currently waiting for a previous operation to complete.");
				}
			}
			catch (IllegalArgumentException|ServerException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
			}
			catch (ConnectException|UnmarshalException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mConnection to server lost");
			}
			catch (Exception e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
				e.printStackTrace();
			}
		}
	}

	public void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException{
		execute(cmd, arguments, Transaction.getDummyTransaction());
	}
	public void execute(Command cmd, Vector<String> arguments, Transaction transaction) throws RemoteException, NumberFormatException, InvalidTransactionException, TransactionAbortedException, TransactionAlreadyWaitingException
	{
		int xid = transaction.getXid();
		switch (cmd)
		{
			case Help:
			{
				if (arguments.size() == 1) {
					System.out.println(Command.description());
				} else if (arguments.size() == 2) {
					Command l_cmd = Command.fromString((String)arguments.elementAt(1));
					System.out.println(l_cmd.toString());
				} else {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
				}
				break;
			}
			case Start:
			{
				checkArgumentsCount(1,arguments.size());
				transaction.setXid(m_resourceManager.start());
				transactionLayerTimer.start(xid);
				System.out.println("Your transaction id is " + xid);
				break;
			}
			case Commit:
			{
				checkArgumentsCount(2, arguments.size());
				
				timer.commit(xid);
				transactionLayerTimer.stop(xid);
				transactionLayerTimer.commit(xid);
				boolean successfullyCommitted = m_resourceManager.commit(xid);
				transaction.setCommitted(successfullyCommitted);
				if (successfullyCommitted){
					System.out.println("Transaction commited successfully.");
				}
				else {
					System.out.println("Failed to commit transaction.");
				}
				break;
			}
			case Abort:
			{
				checkArgumentsCount(2, arguments.size());
				transaction.setAborted(true);
				m_resourceManager.abort(xid);
				transactionLayerTimer.cleanUp(xid);
				System.out.println("Transaction aborted.");
				break;
			}
			case Shutdown:
			{
				checkArgumentsCount(1, arguments.size());
				if (m_resourceManager.shutdown()){
					System.out.println("All servers successfully shutdown, client quitting.");
					System.exit(0);
				}
				else {
					System.out.println("Failed to shutdown servers.");
				}
				break;
			}
			case AddFlight: {
				timer.start(xid);
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding a new flight [xid=" + xid + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				System.out.println("-Flight Seats: " + arguments.elementAt(3));
				System.out.println("-Flight Price: " + arguments.elementAt(4));

				
				int flightNum = toInt(arguments.elementAt(2));
				int flightSeats = toInt(arguments.elementAt(3));
				int flightPrice = toInt(arguments.elementAt(4));

				timer.stop(xid);
				if (m_resourceManager.addFlight(xid, flightNum, flightSeats, flightPrice)) {
					System.out.println("Flight added");
				} else {
					System.out.println("Flight could not be added");
				}
				break;
			}
			case AddCars: {
				timer.start(xid);
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new cars [xid=" + xid + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				System.out.println("-Number of Cars: " + arguments.elementAt(3));
				System.out.println("-Car Price: " + arguments.elementAt(4));

				
				String location = arguments.elementAt(2);
				int numCars = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				timer.stop(xid);
				if (m_resourceManager.addCars(xid, location, numCars, price)) {
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
				}
				break;
			}
			case AddRooms: {
				timer.start(xid);
				checkArgumentsCount(5, arguments.size());

				System.out.println("Adding new rooms [xid=" + xid + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				System.out.println("-Number of Rooms: " + arguments.elementAt(3));
				System.out.println("-Room Price: " + arguments.elementAt(4));

				String location = arguments.elementAt(2);
				int numRooms = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				timer.stop(xid);
				if (m_resourceManager.addRooms(xid, location, numRooms, price)) {
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
				}
				break;
			}
			case AddCustomer: {
				timer.start(xid);
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer [xid=" + xid + "]");

				int id = toInt(arguments.elementAt(1));
				timer.stop(xid);
				int customer = m_resourceManager.newCustomer(xid);

				System.out.println("Add customer ID: " + customer);
				break;
			}
			case AddCustomerID: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Adding a new customer [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int customerID = toInt(arguments.elementAt(2));

				timer.stop(xid);
				if (m_resourceManager.newCustomer(xid, customerID)) {
					System.out.println("Add customer ID: " + customerID);
				} else {
					System.out.println("Customer could not be added");
				}
				break;
			}
			case DeleteFlight: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a flight [xid=" + xid + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int flightNum = toInt(arguments.elementAt(2));

				timer.stop(xid);
				if (m_resourceManager.deleteFlight(xid, flightNum)) {
					System.out.println("Flight Deleted");
				} else {
					System.out.println("Flight could not be deleted");
				}
				break;
			}
			case DeleteCars: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all cars at a particular location [xid=" + xid + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				String location = arguments.elementAt(2);

				timer.stop(xid);
				if (m_resourceManager.deleteCars(xid, location)) {
					System.out.println("Cars Deleted");
				} else {
					System.out.println("Cars could not be deleted");
				}
				break;
			}
			case DeleteRooms: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting all rooms at a particular location [xid=" + xid + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				String location = arguments.elementAt(2);

				timer.stop(xid);
				if (m_resourceManager.deleteRooms(xid, location)) {
					System.out.println("Rooms Deleted");
				} else {
					System.out.println("Rooms could not be deleted");
				}
				break;
			}
			case DeleteCustomer: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Deleting a customer from the database [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				
				int customerID = toInt(arguments.elementAt(2));

				timer.stop(xid);
				if (m_resourceManager.deleteCustomer(xid, customerID)) {
					System.out.println("Customer Deleted");
				} else {
					System.out.println("Customer could not be deleted");
				}
				break;
			}
			case QueryFlight: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying a flight [xid=" + xid + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int flightNum = toInt(arguments.elementAt(2));

				timer.stop(xid);
				int seats = m_resourceManager.queryFlight(xid, flightNum);
				System.out.println("Number of seats available: " + seats);
				break;
			}
			case QueryCars: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars location [xid=" + xid + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				
				String location = arguments.elementAt(2);

				timer.stop(xid);
				int numCars = m_resourceManager.queryCars(xid, location);
				System.out.println("Number of cars at this location: " + numCars);
				break;
			}
			case QueryRooms: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms location [xid=" + xid + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				
				String location = arguments.elementAt(2);

				timer.stop(xid);
				int numRoom = m_resourceManager.queryRooms(xid, location);
				System.out.println("Number of rooms at this location: " + numRoom);
				break;
			}
			case QueryCustomer: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying customer information [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int customerID = toInt(arguments.elementAt(2));

				timer.stop(xid);
				String bill = m_resourceManager.queryCustomerInfo(xid, customerID);
				System.out.print(bill);
				break;               
			}
			case QueryFlightPrice: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());
				
				System.out.println("Querying a flight price [xid=" + xid + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int flightNum = toInt(arguments.elementAt(2));

				timer.stop(xid);
				int price = m_resourceManager.queryFlightPrice(xid, flightNum);
				System.out.println("Price of a seat: " + price);
				break;
			}
			case QueryCarsPrice: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying cars price [xid=" + xid + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				String location = arguments.elementAt(2);

				timer.stop(xid);
				int price = m_resourceManager.queryCarsPrice(xid, location);
				System.out.println("Price of cars at this location: " + price);
				break;
			}
			case QueryRoomsPrice: {
				timer.start(xid);
				checkArgumentsCount(3, arguments.size());

				System.out.println("Querying rooms price [xid=" + xid + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				String location = arguments.elementAt(2);

				timer.stop(xid);
				int price = m_resourceManager.queryRoomsPrice(xid, location);
				System.out.println("Price of rooms at this location: " + price);
				break;
			}
			case ReserveFlight: {
				timer.start(xid);
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving seat in a flight [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Flight Number: " + arguments.elementAt(3));

				int customerID = toInt(arguments.elementAt(2));
				int flightNum = toInt(arguments.elementAt(3));

				timer.stop(xid);
				if (m_resourceManager.reserveFlight(xid, customerID, flightNum)) {
					System.out.println("Flight Reserved");
				} else {
					System.out.println("Flight could not be reserved");
				}
				break;
			}
			case ReserveCar: {
				timer.start(xid);
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a car at a location [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Car Location: " + arguments.elementAt(3));

				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				timer.stop(xid);
				if (m_resourceManager.reserveCar(xid, customerID, location)) {
					System.out.println("Car Reserved");
				} else {
					System.out.println("Car could not be reserved");
				}
				break;
			}
			case ReserveRoom: {
				timer.start(xid);
				checkArgumentsCount(4, arguments.size());

				System.out.println("Reserving a room at a location [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Room Location: " + arguments.elementAt(3));
				
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				timer.stop(xid);
				if (m_resourceManager.reserveRoom(xid, customerID, location)) {
					System.out.println("Room Reserved");
				} else {
					System.out.println("Room could not be reserved");
				}
				break;
			}
			case Bundle: {
				timer.start(xid);
				if (arguments.size() < 7) {
					System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle [xid=" + xid + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					System.out.println("-Flight Number: " + arguments.elementAt(3+i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size()-3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size()-2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size()-1));

				int customerID = toInt(arguments.elementAt(2));
				Vector<String> flightNumbers = new Vector<String>();
				for (int i = 0; i < arguments.size() - 6; ++i)
				{
					flightNumbers.addElement(arguments.elementAt(3+i));
				}
				String location = arguments.elementAt(arguments.size()-3);
				boolean car = toBoolean(arguments.elementAt(arguments.size()-2));
				boolean room = toBoolean(arguments.elementAt(arguments.size()-1));

				timer.stop(xid);
				if (m_resourceManager.bundle(xid, customerID, flightNumbers, location, car, room)) {
					System.out.println("Bundle Reserved");
				} else {
					System.out.println("Bundle could not be reserved");
				}
				break;
			}
			case QueryTransactionResponseTime: {
				Vector<DataPoint> dataPoints = m_resourceManager.queryTransactionResponseTime(xid, new Vector<DataPoint>());
				EnumMap<LayerTypes, Integer> totalTime = new EnumMap<LayerTypes, Integer>(LayerTypes.class);
				EnumMap<LayerTypes, Integer> totalCount = new EnumMap<LayerTypes, Integer>(LayerTypes.class);
				for(Client c : clients){
					dataPoints.add(c.getAverageResponseTime());
					dataPoints.add(c.getTransactionResponseTime());
				}
				for(DataPoint dp : dataPoints){
					int timeSoFar = totalTime.containsKey(dp.getLayer()) ? totalTime.get(dp.getLayer()) : 0;
					int countSoFar = totalCount.containsKey(dp.getLayer()) ? totalCount.get(dp.getLayer()) : 0;
					totalTime.put(dp.getLayer(), timeSoFar + dp.getTotalTime());
					totalCount.put(dp.getLayer(), countSoFar + dp.getTotalCount());
				}


				totalCount.put(LayerTypes.COMMUNICATION, totalCount.get(LayerTypes.TRANSACTION));
				int communicationTime = 0;
				int otherTime = 0;
				for(LayerTypes layer : totalTime.keySet()){
					if(layer == LayerTypes.TRANSACTION){
						continue;
					}
					otherTime += totalTime.get(layer);
				}

				communicationTime = totalTime.get(LayerTypes.TRANSACTION) - otherTime;
				totalTime.put(LayerTypes.COMMUNICATION, Math.max(communicationTime, 0));

				String output = "";

				for(LayerTypes layer : totalTime.keySet()){
					int time = totalTime.get(layer);;
					int count = totalCount.get(layer);
					double average = (count == 0 || time == 0) ? 0 : (double) time / (double) count;
					output += String.format("%15s\t\t\t Transactions: %8d \t\t Total Time: %8d \t\t Average Response Time: %8.3f ms\n", layer.name(), count, time, average);
				}
				output += String.format("%15s\t\t\t%d Transactions/second\n", "THROUGHPUT", 1000 * totalCount.get(LayerTypes.TRANSACTION) / totalTime.get(LayerTypes.TRANSACTION));
				out.println(output);
				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());

				System.out.println("Quitting client");
				System.exit(0);
		}
	}


	public DataPoint getTransactionResponseTime(){
		return transactionLayerTimer.getDataPoint(LayerTypes.TRANSACTION);
	}

	public DataPoint getAverageResponseTime(){
		return timer.getDataPoint(LayerTypes.CLIENT);
	}

	public static Vector<String> parse(String command)
	{
		Vector<String> arguments = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(command,",");
		String argument = "";
		while (tokenizer.hasMoreTokens())
		{
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
	}

	public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException
	{
		if (expected != actual)
		{
			throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
		}
	}

	public static int toInt(String string) throws NumberFormatException
	{
		return (Integer.valueOf(string)).intValue();
	}

	public static boolean toBoolean(String string)// throws Exception
	{
		return (Boolean.valueOf(string)).booleanValue();
	}
}
