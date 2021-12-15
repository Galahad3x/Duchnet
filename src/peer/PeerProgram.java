package peer;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

/**
 * Main program to run a node
 */
public class PeerProgram {
    /**
     * Execute a node
     *
     * @param args args of the program [own_port] [other_ip] [other_port]
     * @throws Exception if something fails
     */
    public static void main(String[] args) throws Exception {
        // configurar registry ip i registry port
        String own_port = "1099";
        String other_ip = null;
        String other_port = "1099";
        if (args.length == 0) {
            System.out.println("Creating isolated node using port 1099...");
        } else if (args.length == 1) {
            System.out.println("Creating isolated node using port " + args[0] + "...");
            own_port = args[0];
        } else if (args.length == 2) {
            System.out.println("Creating node connected to " + args[1] + ":1099");
            own_port = args[0];
            other_ip = args[1];
        } else {
            System.out.println("Creating node connected to " + args[1] + ":" + args[2]);
            own_port = args[0];
            other_ip = args[1];
            other_port = args[2];
        }
        PeerImp peer;
        if (other_ip != null) {
            PeerInfo other_peer_info = new PeerInfo(other_ip, Integer.parseInt(other_port));
            peer = new PeerImp(other_peer_info);
        } else {
            peer = new PeerImp();
        }
        Registry reg = startRegistry(Integer.parseInt(own_port));
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        String ip_str = null;
        while (e.hasMoreElements()) {
            NetworkInterface n = e.nextElement();
            if (n.getName().equals("enp2s0") || n.getName().equals("eth0") || n.getName().equals("wlan0")) {
                Enumeration<InetAddress> adresses = n.getInetAddresses();
                do {
                    ip_str = adresses.nextElement().getHostAddress();
                } while (!ip_str.startsWith("1"));
            } else {
                ip_str = "127.0.0.1";
            }
        }
        peer.start(new PeerInfo(ip_str, Integer.parseInt(own_port)), reg);
    }

    /**
     * @param port port in which to create the registry
     * @return the created registry
     * @throws RemoteException if the remote connection fails
     */
    private static Registry startRegistry(Integer port)
            throws Exception {
        if (port == null) {
            port = 1099;
        }
        try {
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list();
            // The above call will throw an exception
            // if the registry does not already exist
            throw new Exception("Registry port already in use");
        } catch (RemoteException ex) {
            // No valid registry at that port.
            System.out.println("RMI registry cannot be located ");
            Registry registry = LocateRegistry.createRegistry(port);
            System.out.println("RMI registry created at port " + port);
            return registry;
        }
    }
}

