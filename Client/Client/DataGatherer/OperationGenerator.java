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

    public static Operation generateStartOperation(){
        Command cmd = Command.Start;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());

        return new Operation(cmd, arguments);
    }

    public static Operation generateCommitOperation(int xid){
        Command cmd = Command.Commit;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());
        arguments.add(String.valueOf(xid));
        return new Operation(cmd, arguments);
    }

    public static Operation generateAbortOperation(int xid){
        Command cmd = Command.Abort;
        Vector<String> arguments = new Vector<String>();
        arguments.add(cmd.name());
        arguments.add(String.valueOf(xid));
        return new Operation(cmd, arguments);
    }

}