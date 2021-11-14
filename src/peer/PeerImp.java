package peer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class PeerImp extends UnicastRemoteObject implements Peer {
    private final List<PeerInfo> saved_peers_info = new ArrayList<>();
    private final HashMap<String, Peer> saved_peers = new HashMap<>();
    private final HashMap<String, Manager> saved_managers = new HashMap<>();
    private PeerInfo own_info;

    private ContentManager manager;
    public Registry registry;

    protected PeerImp() throws RemoteException {
    }

    protected PeerImp(PeerInfo node_peer_info) throws RemoteException {
        this.saved_peers_info.add(node_peer_info);
    }

    public void add_node(PeerInfo info) throws RemoteException{
        try {
            this.add_node_components(info);
            this.saved_peers_info.add(info);
        } catch (NotBoundException e) {
            System.out.println("Something was not found: Node has been created isolated");
        }
    }

    public void add_node_components(PeerInfo node_peer_info) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(node_peer_info.ip, node_peer_info.port);
        Peer peer = (Peer) registry.lookup("peer");
        Manager manager = (Manager) registry.lookup("manager");
        this.saved_peers.put(node_peer_info.toString(), peer);
        this.saved_managers.put(node_peer_info.toString(), manager);
    }

    public void start(PeerInfo own_info, Registry reg) throws IOException, NotBoundException {
        // configurar registry ip i registry port
        this.own_info = own_info;
        this.registry = reg;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please type the route to the files: ");
        this.manager = new ContentManager(scanner.nextLine());
        // /home/joel/Escriptori/DC/Duchnet/Files1
        this.registry.rebind("manager", this.manager);
        this.registry.rebind("peer", this);
        if (saved_peers_info.size() > 0){
            add_node_components(saved_peers_info.get(0));
            Peer original_peer = saved_peers.get(saved_peers_info.get(0).toString());
            original_peer.add_node(this.own_info);
        }
        this.manager.list_files(false);
        System.out.println("Peer started successfully at " + own_info.ip + ":" + own_info.port.toString());
        this.service_loop();
    }

    /**
     * Service loop of the peer, runs forever until closed by the user
     */
    public void service_loop() throws RemoteException {
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
                    List<Content> network_contents = this.list_files_all_network(new LinkedList<>());
                    this.manager.print_contents(network_contents);
            }
        }
    }

    /**
     * List all the files available in the network
     */
    public List<Content> list_files_all_network(List<PeerInfo> visited_peers) throws RemoteException {
        for (PeerInfo info : visited_peers){
            if(info.toString().equals(this.own_info.toString())){
                return new LinkedList<>();
            }
        }
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        List<Content> found_contents = new LinkedList<>(this.manager.getContents());
        for (PeerInfo peer_info : saved_peers_info) {
            if (peer_info.equals(own_info)) {
                return found_contents;
            }
            Peer peer = saved_peers.get(peer_info.toString());
            // System.out.println("Adding from " + peer_info.toString());
            ContentManager.merge_lists(found_contents, peer.list_files_all_network(new_visited_peers));
        }
        return found_contents;
    }

    /**
     * All the process needed to download a file 1st level
     */
    public void download_file() throws RemoteException {
        // Llistar els fitxers
        List<Content> network_contents = this.list_files_all_network(new LinkedList<>());
        this.manager.print_contents(network_contents);
        // El usuari trie el que vol
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the filename to download, leave blank to cancel: ");
        String filename = scanner.nextLine();
        if (filename.equals("")){
            return;
        }
        // TODO acabar aquest metode
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
