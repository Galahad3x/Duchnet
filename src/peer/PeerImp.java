package peer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class PeerImp extends UnicastRemoteObject implements Peer {

    /**
     * List of all the peers that the program has the Peers and Managers referenced
     */
    private final List<PeerInfo> saved_peers_info = new ArrayList<>();
    /**
     * HashMap where Peer references are stored using their PeerInfo.toString();
     */
    private final HashMap<String, Peer> saved_peers = new HashMap<>();
    /**
     * HashMap where Manager references are stored using their PeerInfo.toString();
     */
    private final HashMap<String, Manager> saved_managers = new HashMap<>();

    /**
     * The PeerInfo of this peer
     */
    private PeerInfo own_info;
    /**
     * The Manager of this peer
     */
    private ContentManager manager;
    /**
     * The Registry of this peer
     */
    public Registry registry;

    /**
     * Constructor used for isolated nodes
     * @throws RemoteException When Remote calls fail
     */
    protected PeerImp() throws RemoteException {
    }

    /**
     * Constructor used for nodes connected to another node
     * @param node_peer_info the PeerInfo of the other node
     * @throws RemoteException When remote calls fail
     */
    protected PeerImp(PeerInfo node_peer_info) throws RemoteException {
        this.saved_peers_info.add(node_peer_info);
    }

    /**
     * Add the Peer and Manager of another node, not remote
     * @param info The PeerInfo of this other node
     * @throws RemoteException When remote calls fail
     */
    public void add_node(PeerInfo info) throws RemoteException {
        try {
            this.add_node_components(info);
            this.saved_peers_info.add(info);
        } catch (NotBoundException e) {
            System.out.println("Something was not found: Node has been created isolated");
        }
    }

    /**
     * Locally: Fetch the Peer and Manager of another node
     * Remotely: When called back remotely with this.own_info as a parameter,
     * used for offering its own Peer and Manager to a node
     * @param node_peer_info the PeerInfo of the offered Peer and Manager
     * @throws RemoteException when remote calls fail
     * @throws NotBoundException when "peer" and "manager" are not in node_peer_info's registry
     */
    public void add_node_components(PeerInfo node_peer_info) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(node_peer_info.ip, node_peer_info.port);
        Peer peer = (Peer) registry.lookup("peer");
        Manager manager = (Manager) registry.lookup("manager");
        this.saved_peers.put(node_peer_info.toString(), peer);
        this.saved_managers.put(node_peer_info.toString(), manager);
    }

    /**
     * Setup function for a node;
     * Locate the files and bind the own Peer and Manager
     * @param own_info the PeerInfo of the node we are building
     * @param reg the Registry of the node we are building
     * @throws IOException if the Scanner fails
     * @throws NotBoundException if looking up a remote node fails
     */
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
        if (saved_peers_info.size() > 0) {
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
     * Listens for commands
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
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "help":
                    System.out.println("quit,list,help,modify,list_all");
                    break;
                case "modify":
                    this.manager.list_files(true);
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "list_all":
                    List<Content> network_contents = this.list_files_all_network(new LinkedList<>());
                    this.manager.print_contents(network_contents);
                    break;
                case "download":
                    try {
                        download_file();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    /**
     * Lists all the files available in the network, called back recursively
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @return List with all the contents with file_data = null;
     * @throws RemoteException if remote calls fail
     */
    public List<Content> list_files_all_network(List<PeerInfo> visited_peers) throws RemoteException {
        for (PeerInfo info : visited_peers) {
            if (info.toString().equals(this.own_info.toString())) {
                return new LinkedList<>();
            }
        }
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        this.manager.list_files(false);
        List<Content> found_contents = new LinkedList<>(this.manager.getContents());
        for (PeerInfo peer_info : saved_peers_info) {
            if (peer_info.equals(own_info)) {
                return found_contents;
            }
            Peer peer = saved_peers.get(peer_info.toString());
            // System.out.println("Adding from " + peer_info.toString());
            ContentManager.merge_lists(found_contents, peer.list_files_all_network(new_visited_peers));
        }
        List<Content> polished_contents = new LinkedList<>();
        for (Content cnt : found_contents){
            if (!polished_contents.contains(cnt)){
                polished_contents.add(cnt);
            }
        }
        return polished_contents;
    }

    /**
     * Find nodes that own a certain file
     * @param file the file we are looking for
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @return List of all the PeerInfo's that own the file
     * @throws RemoteException if remote calls fail
     */
    public List<PeerInfo> find_seed(Content file, List<PeerInfo> visited_peers) throws RemoteException {
        for (PeerInfo info : visited_peers) {
            if (info.toString().equals(this.own_info.toString())) {
                return new LinkedList<>();
            }
        }
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        List<Content> user_contents = new LinkedList<>(this.manager.getContents());
        List<PeerInfo> possible_seeders = new LinkedList<>();
        for (Content content : user_contents){
            if (content.getHash().equals(file.getHash())){
                possible_seeders.add(this.own_info);
            }
        }
        for (PeerInfo peer_info : saved_peers_info) {
            Peer peer = saved_peers.get(peer_info.toString());
            List<PeerInfo> peer_result = peer.find_seed(file, new_visited_peers);
            for (PeerInfo peer_found_seeder : peer_result){
                if (!possible_seeders.contains(peer_found_seeder)){
                    possible_seeders.add(peer_found_seeder);
                }
            }
        }
        return possible_seeders;
    }

    /**
     * All the process needed to download a file 1st level;
     * First list all contents in the network
     * The user choses a file
     * Then find possible seeders in the network
     * Finally download the file from a chosen seeder
     */
    public void download_file() throws Exception {
        // Llistar els fitxers
        List<Content> network_contents = this.list_files_all_network(new LinkedList<>());
        this.manager.print_contents(network_contents);
        // El usuari trie el que vol
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the filename to download, leave blank to cancel: ");
        String filename = scanner.nextLine();
        Content file_to_download = null;
        if (filename.equals("")) {
            return;
        } else {
            List<Content> files_to_download = new LinkedList<>();
            for (Content content : network_contents) {
                for (String name : content.getFilenames()) {
                    if (name.equals(filename)) {
                        files_to_download.add(content);
                    }
                }
            }
            if (files_to_download.size() > 1) {
                System.out.println("Careful: More than one file with the same name!");
                this.manager.print_contents(files_to_download);
                System.out.println("Choose the one you want using the hash: ");
                String chosen_hash = scanner.nextLine();
                for (Content content : files_to_download) {
                    if (content.getHash().startsWith(chosen_hash)) {
                        file_to_download = content;
                    }
                }
            } else if (files_to_download.size() == 0) {
                return;
            } else {
                file_to_download = files_to_download.get(0);
            }
        }
        List<PeerInfo> seeders = find_seed(file_to_download, new LinkedList<>());
        assert seeders.size() > 0;
        if(seeders.contains(this.own_info)){
            System.out.println("You already own this file! Aborting...");
            return;
        }
        Manager seed_manager = null;
        for(PeerInfo peer_info : seeders){
            if (this.saved_peers_info.contains(peer_info)){
                seed_manager = saved_managers.get(peer_info.toString());
            }
        }
        if (seed_manager == null){
            add_node_components(seeders.get(0));
            seed_manager = saved_managers.get(seeders.get(0).toString());
        }
        assert file_to_download != null;
        String file_location = this.manager.getFolder_route() + "/" + filename;
        System.out.println("Starting to download the file... ");
        Content downloaded_file = seed_manager.download_file(file_to_download.getHash());
        try (FileOutputStream stream = new FileOutputStream(file_location)) {
            stream.write(downloaded_file.getFile_data());
        }
        downloaded_file.setFile_data(null);
        this.manager.add_content(downloaded_file);
        System.out.println("File downloaded!");
    }
}
