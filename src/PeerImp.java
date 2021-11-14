import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class PeerImp implements Remote, Peer {
    private final List<PeerInfo> saved_peers_info = new ArrayList<>();
    private final HashMap<PeerInfo, Peer> saved_peers = new HashMap<>();
    private final HashMap<PeerInfo, Manager> saved_managers = new HashMap<>();
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
        this.registry.rebind("manager", this.manager);
        this.registry.rebind("peer", this);
        System.out.println("Peer started successfully at " + own_info.ip + ":" + own_info.port.toString());
        this.manager.list_files(false);
        this.service_loop();
    }

    /**
     * Service loop of the peer, runs forever until closed by the user
     */
    public void service_loop() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Please type a command: ");
            String command = scanner.nextLine();
            switch (command.toLowerCase()) {
                case "quit":
                    System.out.println("Quitting...");
                    System.exit(0);
                case "list":
                    this.manager.list_files(false);
                    break;
                case "help":
                    System.out.println("quit,list,help");
                    break;
                case "modify":
                    this.manager.list_files(true);
                    break;
                case "list_all":
                    this.list_files_all_network(new LinkedList<>());
            }
        }
    }

    /**
     * List all the files available in the network
     */
    public void list_files_all_network(List<PeerInfo> visited_peers) {
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        List<Content> found_contents = new LinkedList<>();
        for(PeerInfo peer_info : saved_peers_info){
            Peer peer = saved_peers.get(peer_info);
            // found_contents.addAll(peer.list_files_all_network(new_visited_peers));
        }
    }

    /**
     * All the process needed to download a file 1st level
     */
    public void download_file() {
        // Llistar els fitxers
        // El usuari trie el que vol
        // Si tenim ip i port/ contentmanager remot guardat el fem servir
        // Sino u demanem
    }

    /**
     * Share the own IP and PORT to another node
     */
    public PeerInfo share_address() {
        if (own_info.ip.equals("")) {
            return null;
        }
        return this.own_info;
    }
}
