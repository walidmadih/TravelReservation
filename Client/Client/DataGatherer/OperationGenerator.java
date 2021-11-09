package Client.DataGatherer;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Vector;

import Client.Command;
import Server.Interface.IResourceManager;

public class OperationGenerator{

    private static int maxOperationsCount = 10;

    private static int maxAdd = 10;
    private static int maxFlightNum = 10;
    private static int maxPrice = 5000;
    private static int maxCustomerID = 500;

    private enum Cities{
        BANGKOK, PARIS, LONDON, DUBAI, SINGAPORE, KUALA, NEWYORK, INSTANBUL, TOKYO, ANTALYA
    }

    private enum Resources{
        FLIGHT, ROOMS, CARS;
    }

    private enum TransactionTypes{
        ADD, DELETE, QUERY, QUERYPRICE, RESERVE
    }

    public static Operation generateRandomOperation(){
        Resources resource = Resources.values()[(int)(Math.random()*Resources.values().length)];
        TransactionTypes transactionType = TransactionTypes.values()[(int)(Math.random()*TransactionTypes.values().length)];

        String commandName = transactionType == TransactionTypes.QUERYPRICE ? TransactionTypes.QUERY.name() + resource.name() + "Price" : transactionType.name() + resource.name();
        if(transactionType == TransactionTypes.RESERVE && (resource == Resources.ROOMS || resource == Resources.CARS)){
            commandName = commandName.substring(0, commandName.length() - 1);
        }
        Command operationCommand = Command.fromString(commandName);

        Vector<String> arguments = new Vector<String>();

        String quantity = String.valueOf((int)(Math.random()*maxAdd));
        String flightnum_location = resource == Resources.FLIGHT ? String.valueOf((int)(Math.random()*maxFlightNum)) : Cities.values()[(int)(Math.random()*Cities.values().length)].name();
        String price = String.valueOf((int)(Math.random()*maxPrice));
        String customerId = String.valueOf((int)(Math.random()*maxCustomerID));

        arguments.add(operationCommand.name());

        switch(transactionType){
            case ADD: {
                arguments.add(flightnum_location);
                arguments.add(quantity);
                arguments.add(price);
                break;
            }
            case DELETE: {
                arguments.add(flightnum_location);
                break;
            }
            case QUERY: {
                arguments.add(flightnum_location);
                break;
            }
            case QUERYPRICE: {
                arguments.add(flightnum_location);
                break;
            }
            case RESERVE: {
                arguments.add(customerId);
                arguments.add(flightnum_location);
            }
        }

        return new Operation(operationCommand, arguments);
    }

    public static LinkedList<Operation> generateRandomOperations(){
        LinkedList<Operation> operations = new LinkedList<>();
        int operationsCount = (int) (Math.random()*(maxOperationsCount - 1) + 1);
        for(int i = 0; i < operationsCount; i++){
            operations.add(generateRandomOperation());
        }
        return operations;
    }

    public static LinkedList<Operation> generateTimeDataTransaction(){
        LinkedList<Operation> ll =  new LinkedList<Operation>();
        Command cmd = Command.QueryTransactionResponseTime;
        Vector<String> arguments = new Vector<>();
        arguments.add(cmd.name());
        ll.add(new Operation(cmd, arguments));
        return ll;
    }

    public static LinkedList<Operation> generateflightOnlyTransaction(int flightNum, int numSeats, int price)
    {
        LinkedList<Operation> ops = new LinkedList<>();
        Vector<Command> cmds = new Vector<>();

        cmds.add(Command.AddFlight);
        cmds.add(Command.QueryFlight);
        cmds.add(Command.QueryFlightPrice);
        cmds.add(Command.DeleteFlight);
        
        for (int i = 0; i < 4; i++)
        {
            Command cmd = cmds.get(i);
            for (int j = 0; j < 3; j++)
            {
                Vector<String> args = new Vector<>();
                args.add(cmd.name());
                args.add(String.valueOf(flightNum + j));

                if (cmd == Command.AddFlight)
                {
                    args.add(String.valueOf(numSeats));
                    args.add(String.valueOf(price));
                }

                ops.add(new Operation(cmd, args));
            }
        }
        return ops;
    }

    public static LinkedList<Operation> generateAllRMTransaction(int flightNum, String location, int numItem, int price)
    {
        LinkedList<Operation> ops = new LinkedList<>();
        Vector<Command> cmds = new Vector<>();

        cmds.add(Command.AddFlight);
        cmds.add(Command.QueryFlight);
        cmds.add(Command.QueryFlightPrice);
        cmds.add(Command.DeleteFlight);
        cmds.add(Command.AddCars);
        cmds.add(Command.QueryCars);
        cmds.add(Command.QueryCarsPrice);
        cmds.add(Command.DeleteCars);
        cmds.add(Command.AddRooms);
        cmds.add(Command.QueryRooms);
        cmds.add(Command.QueryRoomsPrice);
        cmds.add(Command.DeleteRooms);

        for (int i = 0; i < 12; i++)
        {
            Command cmd = cmds.get(i);
            Vector<String> args = new Vector<>();
            args.add(cmd.name());

            if (i < 4)
                args.add(String.valueOf(flightNum));
            else
                args.add(location);

            if (cmd == Command.AddFlight || cmd == Command.AddCars || cmd == Command.AddRooms)
            {
                args.add(String.valueOf(numItem));
                args.add(String.valueOf(price));
            }

            ops.add(new Operation(cmd, args));
        }
        return ops;
    }

}