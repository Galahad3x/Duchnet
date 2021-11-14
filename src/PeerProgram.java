import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PeerProgram{
    public static void main(String[] args) throws Exception {
        // configurar registry ip i registry port
        if(args.length <= 1){
            System.out.println("Creating isolated node...");
        }else if (args.length == 2){
            System.out.println("Creating node connected to " + args[1] + ":1099");
        }else{
            System.out.println("Creating node connected to " + args[1] + ":" + args[2]);
        }
        String own_port = args[0];
        String other_ip = args[1];
        String other_port = args[2];
        Registry reg = startRegistry(Integer.parseInt(own_port));
        PeerImp peer = new PeerImp();
        InetAddress ia = InetAddress.getLocalHost();
        String ip_str = ia.getHostAddress();
        peer.start(new PeerInfo(ip_str, Integer.parseInt(own_port)),reg);
    }

    /**
     * @param port port in which to create the registry
     * @return the created registry
     * @throws RemoteException if the remote connection fails
     */
    private static Registry startRegistry(Integer port)
            throws Exception {
        if(port == null) {
            port = 1099;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list( );
            // The above call will throw an exception
            // if the registry does not already exist
            throw new Exception("Registry port already in use");
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

