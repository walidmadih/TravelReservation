package Client;

import java.net.Socket;
import java.util.*;
import java.io.*;
import java.util.concurrent.ExecutionException;

import Server.Interface.*;

public abstract class Client
{
	Socket clientSocket;
	ObjectOutputStream objectOutputStream;
	ObjectInputStream objectInputStream;

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
				execute(cmd, arguments);
				objectOutputStream.flush();
			}
			catch (IllegalArgumentException e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0m" + e.getLocalizedMessage());
			}
			catch (Exception e) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 + "[0mUncaught exception");
				e.printStackTrace();
			}
		}
	}

	public void execute(Command cmd, Vector<String> arguments) throws IOException, NumberFormatException, ClassNotFoundException {
		RemoteMethod remoteMethod = new RemoteMethod(cmd, arguments);
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
			case AddFlight: {
				if (5 != arguments.size()) {
					break;
				}

				System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				System.out.println("-Flight Seats: " + arguments.elementAt(3));
				System.out.println("-Flight Price: " + arguments.elementAt(4));

				/*
				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));
				int flightSeats = toInt(arguments.elementAt(3));
				int flightPrice = toInt(arguments.elementAt(4));
				*/

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Flight added");
				} else {
					System.out.println("Flight could not be added");
				}

				break;
			}
			case AddCars: {
				if (5 != arguments.size()) {
					break;
				}

				System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));
				System.out.println("-Number of Cars: " + arguments.elementAt(3));
				System.out.println("-Car Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numCars = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Cars added");
				} else {
					System.out.println("Cars could not be added");
				}
				break;
			}
			case AddRooms: {
				if (5 != arguments.size()) {
					break;
				}

				System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));
				System.out.println("-Number of Rooms: " + arguments.elementAt(3));
				System.out.println("-Room Price: " + arguments.elementAt(4));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);
				int numRooms = toInt(arguments.elementAt(3));
				int price = toInt(arguments.elementAt(4));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Rooms added");
				} else {
					System.out.println("Rooms could not be added");
				}
				break;
			}
			case AddCustomer: {
				if (2 != arguments.size()) {
					break;
				}

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

				int id = toInt(arguments.elementAt(1));
				int customer = (int) getRemoteResponse(remoteMethod);

				System.out.println("Add customer ID: " + customer);
				break;
			}
			
			case AddCustomerID: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Add customer ID: " + customerID);
				} else {
					System.out.println("Customer could not be added");
				}
				break;
			}
			
			case DeleteFlight: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Flight Deleted");
				} else {
					System.out.println("Flight could not be deleted");
				}
				break;
			}
			case DeleteCars: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Cars Deleted");
				} else {
					System.out.println("Cars could not be deleted");
				}
				break;
			}
			case DeleteRooms: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Rooms Deleted");
				} else {
					System.out.println("Rooms could not be deleted");
				}
				break;
			}
			case DeleteCustomer: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Customer Deleted");
				} else {
					System.out.println("Customer could not be deleted");
				}
				break;
			}
			case QueryFlight: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				int seats = (int) getRemoteResponse(remoteMethod);
				System.out.println("Number of seats available: " + seats);
				break;
			}
			case QueryCars: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int numCars = (int) getRemoteResponse(remoteMethod);
				System.out.println("Number of cars at this location: " + numCars);
				break;
			}
			case QueryRooms: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int numRoom = (int) getRemoteResponse(remoteMethod);
				System.out.println("Number of rooms at this location: " + numRoom);
				break;
			}
			case QueryCustomer: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));

				String bill = (String) getRemoteResponse(remoteMethod);
				System.out.print(bill);
				break;
			}
			case QueryFlightPrice: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Flight Number: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				int flightNum = toInt(arguments.elementAt(2));

				int price = (int) getRemoteResponse(remoteMethod);
				System.out.println("Price of a seat: " + price);
				break;
			}
			case QueryCarsPrice: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Car Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int price = (int) getRemoteResponse(remoteMethod);
				System.out.println("Price of cars at this location: " + price);
				break;
			}
			case QueryRoomsPrice: {
				if (3 != arguments.size()) {
					break;
				}

				System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Room Location: " + arguments.elementAt(2));

				int id = toInt(arguments.elementAt(1));
				String location = arguments.elementAt(2);

				int price = (int) getRemoteResponse(remoteMethod);
				System.out.println("Price of rooms at this location: " + price);
				break;
			}
			case ReserveFlight: {
				if (4 != arguments.size()) {
					break;
				}

				System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Flight Number: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				int flightNum = toInt(arguments.elementAt(3));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Flight Reserved");
				} else {
					System.out.println("Flight could not be reserved");
				}
				break;
			}
			case ReserveCar: {
				if (4 != arguments.size()) {
					break;
				}

				System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Car Location: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Car Reserved");
				} else {
					System.out.println("Car could not be reserved");
				}
				break;
			}
			case ReserveRoom: {
				if (4 != arguments.size()) {
					break;
				}

				System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				System.out.println("-Room Location: " + arguments.elementAt(3));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				String location = arguments.elementAt(3);

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Room Reserved");
				} else {
					System.out.println("Room could not be reserved");
				}
				break;
			}
			case Bundle: {
				if (arguments.size() < 7) {
					System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
				System.out.println("-Customer ID: " + arguments.elementAt(2));
				for (int i = 0; i < arguments.size() - 6; ++i) {
					System.out.println("-Flight Number: " + arguments.elementAt(3 + i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size() - 3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size() - 2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size() - 1));

				int id = toInt(arguments.elementAt(1));
				int customerID = toInt(arguments.elementAt(2));
				Vector<String> flightNumbers = new Vector<String>();
				for (int i = 0; i < arguments.size() - 6; ++i) {
					flightNumbers.addElement(arguments.elementAt(3 + i));
				}
				String location = arguments.elementAt(arguments.size() - 3);
				boolean car = toBoolean(arguments.elementAt(arguments.size() - 2));
				boolean room = toBoolean(arguments.elementAt(arguments.size() - 1));

				if ((boolean) getRemoteResponse(remoteMethod)) {
					System.out.println("Bundle Reserved");
				} else {
					System.out.println("Bundle could not be reserved");
				}
				break;
			}
			case Quit: {
				checkArgumentsCount(1, arguments.size());
				System.out.println("Quitting client");
				System.exit(0);
				break;
			}
		}

	}

	public Serializable getRemoteResponse(RemoteMethod remoteMethod){
		try {
			objectOutputStream.writeObject(remoteMethod);
			return (Serializable) objectInputStream.readObject();
		}catch (IOException | ClassNotFoundException e ){
			e.printStackTrace();
			System.out.println("There was an error while communicating with the server.");
			return null;
		}
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
