package Server.Middleware;

public class TCPMiddleware extends TCPServer{

    private static int port = 5163;
    private static String name = "Middleware";

    public static void main(String args[]){
        //if (!(args.length == 3))
        //{
        //    throw new IllegalArgumentException("Invalid Argument Count");
        //}




        // Create the RMI server entry
        try {
            TCPMiddleware middleware = new TCPMiddleware();
            middleware.start(port);
        }
        catch (Exception e) {
            System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null)
        {
            System.setSecurityManager(new SecurityManager());
        }
    }

    public TCPMiddleware(){ super(); }
}
