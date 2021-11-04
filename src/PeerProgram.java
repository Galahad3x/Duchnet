import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PeerProgram{
    public static void main(String[] args) throws Exception {
        // configurar registry ip i registry port
        if(args.length < 2){
            throw new Exception("Not enough parameters");
        }
        String ip = args[0];
        Registry reg = startRegistry(Integer.parseInt(args[1]));
        PeerImp peer = new PeerImp();
        peer.start(new PeerInfo(ip, Integer.parseInt(args[1])),reg);
    }

    /**
     * @param port port in which to create the registry
     * @return the created registry
     * @throws RemoteException if the remote connection fails
     */
    private static Registry startRegistry(Integer port)
            throws RemoteException {
        if(port == null) {
            port = 1099;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list( );
            // The above call will throw an exception
            // if the registry does not already exist
            return registry;
        }
        catch (RemoteException ex) {
            // No valid registry at that port.
            System.out.println("RMI registry cannot be located ");
            Registry registry= LocateRegistry.createRegistry(port);
            System.out.println("RMI registry created at port " + port);
            return registry;
        }
    }
}

