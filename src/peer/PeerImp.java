package peer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Semaphore;

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

    public GlobalQueueThread file_queue_thread;
    public GlobalQueueThread download_queue_thread;

    /**
     * Constructor used for isolated nodes
     *
     * @throws RemoteException When Remote calls fail
     */
    protected PeerImp() throws RemoteException {
    }

    /**
     * Constructor used for nodes connected to another node
     *
     * @param node_peer_info the PeerInfo of the other node
     * @throws RemoteException When remote calls fail
     */
    protected PeerImp(PeerInfo node_peer_info) throws RemoteException {
        this.saved_peers_info.add(node_peer_info);
    }

    /**
     * Add the Peer and Manager of another node, not remote
     *
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
     *
     * @param node_peer_info the PeerInfo of the offered Peer and Manager
     * @throws RemoteException   when remote calls fail
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
     *
     * @param own_info the PeerInfo of the node we are building
     * @param reg      the Registry of the node we are building
     * @throws IOException       if the Scanner fails
     * @throws NotBoundException if looking up a remote node fails
     */
    public void start(PeerInfo own_info, Registry reg) throws IOException, NotBoundException {
        // configurar registry ip i registry port
        this.own_info = own_info;
        this.registry = reg;
        int download_threads = 4;
        int upload_threads = 4;
        int file_threads = 4;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please type the route to the files: ");
        String file_route = scanner.nextLine();
        System.out.println("Type the maximum number of download threads: ");
        try {
            download_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Couldn't parse the number, setting default 4");
        }
        System.out.println("Type the maximum number of upload threads: ");
        try {
            upload_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Couldn't parse the number, setting default 4");
        }
        System.out.println("Type the maximum number of file threads: ");
        try {
            file_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Couldn't parse the number, setting default 4");
        }
        Semaphore upload_semaphore = new Semaphore(upload_threads);
        this.manager = new ContentManager(file_route, upload_semaphore);
        this.file_queue_thread = new GlobalQueueThread(file_threads);
        this.download_queue_thread = new GlobalQueueThread(download_threads);
        this.file_queue_thread.start();
        this.download_queue_thread.start();
        // /home/joel/Escriptori/DC/Duchnet/Files1
        this.registry.rebind("manager", this.manager);
        this.registry.rebind("peer", this);
        if (saved_peers_info.size() > 0) {
            add_node_components(saved_peers_info.get(0));
            Peer original_peer = saved_peers.get(saved_peers_info.get(0).toString());
            original_peer.add_node(this.own_info);
        }
        // this.manager.list_files(false);
        System.out.println("Peer started successfully at " + own_info.ip + ":" + own_info.port.toString());
        this.service_loop();
    }

    // TODO Clean and standardize the CLI

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
                    // TODO Tancar els threads que queden oberts
                    System.exit(0);
                case "list":
                    this.manager.list_files(false);
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "help":
                    System.out.println("quit,list,help,modify,list_all,download");
                    break;
                case "modify":
                    this.manager.list_files(true);
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "list_all":
                    List<Content> network_contents = this.find_network_contents(new LinkedList<>(), "name:");
                    this.manager.print_contents(network_contents);
                    break;
                case "download":
                    System.out.println("What do you want to search by? Leave blank for name ");
                    String search_method = scanner.nextLine();
                    System.out.println("What do you want to search for? ");
                    String search_term = scanner.nextLine();
                    try {
                        download_file(search_term, search_method);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    // TODO Error handling this function
    public void download_file(String search_term, String search_method) throws Exception {
        if (search_method.equals("")) {
            search_method = "name";
        }
        List<Content> network_contents = find_network_contents(new LinkedList<>(), search_method + ":" + search_term);
        Content file_to_download = let_user_choose_file(network_contents);
        if (file_to_download == null) {
            System.out.println("Error finding the file, cancelling...");
            return;
        }
        fetch_file(file_to_download, file_to_download.getFilenames().get(0));
    }

    /**
     * Lists all the files available in the network, called back recursively
     *
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @param restriction   Restriction of the returned files, looks like description|tag|name:desired data
     * @return List with all the contents with file_data = null;
     * @throws RemoteException if remote calls fail
     */
    public List<Content> find_network_contents(List<PeerInfo> visited_peers, String restriction) throws RemoteException {
        for (PeerInfo info : visited_peers) {
            if (info.toString().equals(this.own_info.toString())) {
                return new LinkedList<>();
            }
        }
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        this.manager.list_filtered_files(restriction);
        List<Content> found_contents = new LinkedList<>(this.manager.getContents());
        found_contents = this.manager.filter_contents(found_contents, restriction);
        for (PeerInfo peer_info : saved_peers_info) {
            if (peer_info.equals(own_info)) {
                return found_contents;
            }
            Peer peer = saved_peers.get(peer_info.toString());
            // System.out.println("Adding from " + peer_info.toString());
            ContentManager.merge_lists(found_contents, peer.find_network_contents(new_visited_peers, restriction));
        }
        return found_contents;
    }

    /**
     * Find nodes that own a certain file
     *
     * @param file          the file we are looking for
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @return List of all the PeerInfo's that own the file
     * @throws RemoteException if remote calls fail
     */
    public List<PeerInfo> find_seeders(Content file, List<PeerInfo> visited_peers) throws RemoteException {
        for (PeerInfo info : visited_peers) {
            if (info.toString().equals(this.own_info.toString())) {
                return new LinkedList<>();
            }
        }
        List<PeerInfo> new_visited_peers = new LinkedList<>(visited_peers);
        new_visited_peers.add(this.own_info);
        List<Content> user_contents = new LinkedList<>(this.manager.getContents());
        List<PeerInfo> possible_seeders = new LinkedList<>();
        for (Content content : user_contents) {
            if (content.getHash().equals(file.getHash())) {
                possible_seeders.add(this.own_info);
            }
        }
        for (PeerInfo peer_info : saved_peers_info) {
            Peer peer = saved_peers.get(peer_info.toString());
            List<PeerInfo> peer_result = peer.find_seeders(file, new_visited_peers);
            for (PeerInfo peer_found_seeder : peer_result) {
                if (!possible_seeders.contains(peer_found_seeder)) {
                    possible_seeders.add(peer_found_seeder);
                }
            }
        }
        return possible_seeders;
    }

    /**
     * Fetch a file from a remote host
     *
     * @param file_to_download The file to download, without filedata
     * @param filename         The filename to save the file in
     * @throws Exception if something fails
     */
    public void fetch_file(Content file_to_download, String filename) throws Exception {
        List<PeerInfo> seeders = find_seeders(file_to_download, new LinkedList<>());
        assert seeders.size() > 0;
        // Do not transfer a file that is already owned by the user
        if (seeders.contains(this.own_info)) {
            System.out.println("You already own this file! Aborting...");
            return;
        }
        // Request the file from a known seeder if possible
        List<Manager> seed_managers = new LinkedList<>();
        for (PeerInfo peer_info : seeders) {
            if (!this.saved_peers_info.contains(peer_info)) {
                add_node(peer_info);
            }
            seed_managers.add(saved_managers.get(peer_info.toString()));
        }
        assert file_to_download != null;
        String file_location = this.manager.getFolder_route() + "/" + filename;
        System.out.println("Starting to download the file... ");

        // TODO Mirar si sha de partir el fitxer i fer-ho si fa falta

        file_queue_thread.add_thread(new FileQueueThread(file_queue_thread, download_queue_thread, seed_managers, file_to_download.getHash(), file_location));
    }

    /**
     * Get the input of the user to know which file to download
     *
     * @param network_contents contents the user can choose from
     * @return the content the user chose
     */
    public Content let_user_choose_file(List<Content> network_contents) {
        // El usuari trie el que vol
        this.manager.print_contents(network_contents);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the filename to download, leave blank to cancel: ");
        String filename = scanner.nextLine();
        Content file_to_download = null;
        if (filename.equals("")) {
            return null;
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
                return null;
            } else {
                file_to_download = files_to_download.get(0);
            }
        }
        if (file_to_download == null) {
            return null;
        }
        file_to_download.getFilenames().remove(filename);
        file_to_download.getFilenames().add(0, filename);
        return file_to_download;
    }

    public static class GlobalQueueThread extends Thread {
        final Queue<MyThread> queue;
        final MyThread[] active;

        public GlobalQueueThread(int allowed) {
            queue = new LinkedList<>();
            active = new MyThread[allowed];
        }

        @Override
        public void run() {
            try {
                synchronized (this) {
                    while (true) {
                        this.wait();
                        for (int i = 0; i < this.active.length; i++) {
                            if (this.active[i] != null && this.active[i].isFinished()) {
                                this.active[i].join();
                                this.active[i].write_file();
                                this.active[i] = null;
                            }
                            if (this.active[i] == null && !this.queue.isEmpty()) {
                                this.active[i] = this.queue.poll();
                                this.active[i].start();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void alert() {
            synchronized (this) {
                this.notify();
            }
        }

        public void add_thread(MyThread thread) {
            synchronized (this) {
                System.out.println("NEW ELEMENT IN QUEUE");
                queue.add(thread);
                this.notify();
            }
        }
    }

    public static class FileQueueThread extends MyThread {
        final GlobalQueueThread file_queue_thread;
        final GlobalQueueThread download_queue_thread;
        String hash_to_download;
        final List<Manager> seed_managers;
        final ByteSlice[] slices_array;
        final String file_location;

        public FileQueueThread(GlobalQueueThread file_thread, GlobalQueueThread download_thread, List<Manager> seed_managers, String hash_to_download, String file_location) throws Exception {
            this.file_queue_thread = file_thread;
            this.download_queue_thread = download_thread;
            this.seed_managers = seed_managers;
            this.hash_to_download = hash_to_download;
            this.slices_array = new ByteSlice[this.seed_managers.get(0).getSlicesNeeded(hash_to_download)];
            this.file_location = file_location;
        }

        @Override
        public void run() {
            for (int i = 0; i < this.slices_array.length; i++) {
                Manager random_manager = seed_managers.get(new Random().nextInt(seed_managers.size()));
                System.out.println("Adding download to queue ");
                try {
                    download_queue_thread.add_thread(new DownloadThread(download_queue_thread, this, random_manager, i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void write_file() {
            try (FileOutputStream stream = new FileOutputStream(file_location)) {
                for (ByteSlice byteSlice : this.slices_array) {
                    assert byteSlice != null;
                    byte[] bytes = byteSlice.getBytes();
                    for (int j = 0; j < byteSlice.getBytes_written(); j++) {
                        stream.write(bytes[j]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("File " + hash_to_download + " downloaded!");
        }

        public boolean isFinished() {
            for (ByteSlice slice : slices_array) {
                if (slice == null) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class DownloadThread extends MyThread {
        final GlobalQueueThread download_queue_thread;
        final FileQueueThread file_thread;
        final Manager seed_manager;
        int slice_index;
        boolean finished;

        public DownloadThread(GlobalQueueThread download_thread, FileQueueThread file_thread, Manager seed_manager, int slice_index) {
            this.download_queue_thread = download_thread;
            this.seed_manager = seed_manager;
            this.file_thread = file_thread;
            this.slice_index = slice_index;
        }

        @Override
        public void run() {
            System.out.println("STARTING THREAD: " + file_thread.hash_to_download + " slice " + slice_index);
            try {
                ByteSlice result = seed_manager.get_slice(file_thread.hash_to_download, slice_index);
                file_thread.slices_array[slice_index] = result;
                finished = true;
            } catch (Exception e) {
                file_thread.slices_array[slice_index] = null;
                finished = true;
            }
            this.download_queue_thread.alert();
            this.file_thread.file_queue_thread.alert();
            System.out.println("THREAD " + file_thread.hash_to_download + " " + this.slice_index + " is done!");
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void write_file() {

        }
    }
}


