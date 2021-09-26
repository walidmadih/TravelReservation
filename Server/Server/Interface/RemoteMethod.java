package Server.Interface;

import java.io.Serializable;
import java.util.Vector;

public class RemoteMethod implements Serializable {
    private Command command;
    private Vector<String> arguments;

    public RemoteMethod(Command pCommand, Vector<String> pArguments){
        command = pCommand;
        arguments = pArguments;
    }

    public Command getCommand() {
        return command;
    }

    public Vector<String> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(command.toString());
        for (String argument : arguments){
            sb.append("\n");
            sb.append(argument);
        }
        return sb.toString();
    }
}

