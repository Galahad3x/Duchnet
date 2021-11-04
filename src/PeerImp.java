import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PeerImp implements Peer{
    private List<PeerInfo> peers = new ArrayList<>();
    private PeerInfo own_info;

    private ContentManager manager;
    public Registry registry;

    public void start(PeerInfo own_info, Registry reg) throws IOException {
        // configurar registry ip i registry port
        this.own_info = own_info;
        this.registry = reg;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please type the route to the files: ");
        this.manager = new ContentManager(scanner.nextLine());
        // /home/joel/Escriptori/DC/Duchnet/Files1
        this.registry.rebind("registry", this.manager);
        System.out.println("Peer started successfully");
        this.manager.list_files();
        this.service_loop();
    }

    /**
     * Service loop of the peer, runs forever until closed by the user
     */
    public void service_loop(){
        while(true){

        }
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

    /**
     * Get files from a node in the network
     */
    public void list_files(){

    }

    /**
     * List all the files available in the network
     */
    public void list_files_all_network(){

    }

    /**
     * All the process needed to download a file 1st level
     */
    public void download_file(){
        // Llistar els fitxers
        // El usuari trie el que vol
        // Si tenim ip i port/ contentmanager remot guardat el fem servir
        // Sino u demanem
    }

    /**
     * Share the own IP and PORT to another node
     */
    public PeerInfo share_address(){
        if(own_info.ip.equals("")){
            return null;
        }
        return null;
    }
}
