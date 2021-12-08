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
import java.util.logging.*;

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
    protected ContentManager manager;
    /**
     * The Registry of this peer
     */
    public Registry registry;

    public GlobalQueueThread file_queue_thread;
    public GlobalQueueThread download_queue_thread;

    public static Logger logger = Logger.getLogger("peer");

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
        } catch (RemoteException | NotBoundException e) {
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
        System.out.println("Type the route to the files: ");
        String file_route = scanner.nextLine();
        System.out.println("Type the maximum number of download threads: ");
        try {
            download_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            logger.info("Couldn't parse the number, setting default 4");
        }
        System.out.println("Type the maximum number of upload threads: ");
        try {
            upload_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            logger.info("Couldn't parse the number, setting default 4");
        }
        System.out.println("Type the maximum number of file threads: ");
        try {
            file_threads = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            logger.info("Couldn't parse the number, setting default 4");
        }
        Semaphore upload_semaphore = new Semaphore(upload_threads);
        this.manager = new ContentManager(file_route, upload_semaphore, logger);
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
        logger.info("Peer started successfully at " + own_info.ip + ":" + own_info.port.toString());
        logger.setLevel(Level.WARNING);
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
                    // Quits the application and shuts down the node
                    System.out.println("Quitting...");
                    XMLDatabase.write_to_xml(this.manager.getFolder_route(), this.manager.getContents());
                    System.exit(0);
                case "help":
                    // Prints a help message
                    System.out.println("Duchnet HELP");
                    System.out.println("------------------------------");
                    System.out.println("COMMAND\t\t\tDESCRIPTION");
                    System.out.println("QUIT\t\t\tQuits the program");
                    System.out.println("HELP\t\t\tPrint this message");
                    System.out.println("LIST\t\t\tList all files found in your local directory");
                    System.out.println("LIST ALL\t\tList all files found in the network");
                    System.out.println("MODIFY\t\t\tAdd descriptions and tags to your files");
                    System.out.println("DOWNLOAD\t\tStart a download process");
                    System.out.println("DEBUG\t\t\tToggle between INFO and WARNING debug");
                    System.out.println("PROGRESS\t\tShow information about downloads in motion");
                    break;
                case "list":
                    // List files found locally without modifying
                    this.manager.list_files(false);
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "list all":
                    // Lists all files found in the network
                    List<Content> network_contents = this.find_network_contents(new LinkedList<>(), "name:");
                    this.manager.print_contents(network_contents);
                    break;
                case "modify":
                    // List files found locally letting the user add data to them
                    this.manager.list_files(true);
                    this.manager.print_contents(this.manager.getContents());
                    break;
                case "download":
                    // Lets the user search for a file and start its download
                    System.out.println("What do you want to search by? ");
                    String search_method = scanner.nextLine();
                    System.out.println("What do you want to search for? ");
                    String search_term = scanner.nextLine();
                    try {
                        download_file(search_term, search_method);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.severe("Something went wrong while starting a download");
                    }
                    break;
                case "debug":
                    if (logger.getLevel().equals(Level.INFO)) {
                        logger.info("Setting debug level to WARNING");
                        logger.setLevel(Level.WARNING);
                    } else {
                        logger.setLevel(Level.INFO);
                        logger.info("Setting debug level to INFO");
                    }
                    break;
                case "progress":
                    this.file_queue_thread.printProgress();
                    break;
            }
        }
    }

    public void download_file(String search_term, String search_method) throws Exception {
        if (search_method.equals("")) {
            search_method = "name";
        }
        List<Content> network_contents = find_network_contents(new LinkedList<>(), search_method + ":" + search_term);
        Content file_to_download = let_user_choose_file(network_contents);
        if (file_to_download == null) {
            logger.warning("The file you specified has not been found, no download has been started");
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
        if (seeders.size() == 0) {
            logger.severe("The file has no seeders");
            return;
        }
        // Do not transfer a file that is already owned by the user
        if (seeders.contains(this.own_info)) {
            logger.info("You already own this file, aborting download...");
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
        if (file_to_download == null) {
            logger.severe("Something went wrong with the file selection");
            return;
        }
        String file_location;
        logger.info("Adding " + filename + " to the download queue");

        int rand = new Random(seed_managers.size()).nextInt() % seed_managers.size();
        Manager manager = seed_managers.get(rand);
        try {
            Content info = manager.get_information(file_to_download.getHash());
            ContentManager.merge_lists(this.manager.getContents(), Collections.singletonList(info));
        } catch (Exception e) {
            logger.warning("Something failed while retrieving data");
        }

        logger.warning("Going for it");
        List<String> hashes = manager.getHashesNeeded(file_to_download.getHash());

        List<MyThread> threads = new LinkedList<>();
        for (String hash : hashes) {
            file_location = this.manager.getFolder_route() + "/" + manager.get_filename(hash, file_to_download.getFilenames());
            threads.add(new FileQueueThread(file_queue_thread, download_queue_thread, seed_managers, hash, file_location));
        }
        file_queue_thread.add_threads(threads);
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
        boolean running = true;
        final Object lck;

        public GlobalQueueThread(int allowed) {
            queue = new LinkedList<>();
            active = new MyThread[allowed];
            lck = new Object();
        }

        @Override
        public void run() {
            try {
                synchronized (this) {
                    while (running) {
                        logger.warning("Global Queue waits " + this.getName());
                        this.wait();
                        logger.warning("Global Queue owns the critical zone" + this.getName());
                        synchronized (lck) {
                            for (int i = 0; i < this.active.length; i++) {
                                System.err.println("Recorrent " + this.getName());
                                if (this.active[i] != null && this.active[i].isFinished()) {
                                    System.err.println("Primer if " + this.getName() + " " + this.active[i].get_progress());
                                    this.active[i].write_file();
                                    System.err.println("Primer iif " + this.getName());
                                    if (!this.active[i].getState().equals(State.BLOCKED)) {
                                        this.active[i].join();
                                    }else{
                                        continue;
                                    }
                                    System.err.println("Primer iiif " + this.getName());
                                    this.active[i] = null;
                                }
                                if (this.active[i] == null && !this.queue.isEmpty()) {
                                    System.err.println("Segon if " + this.getName());
                                    this.active[i] = this.queue.poll();
                                    logger.warning("New thread from " + this.getName());
                                    this.active[i].create_slice_array();
                                    this.active[i].start();
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.severe("InterruptedException at " + this.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void alert(DownloadThread alerting) {
            logger.info("Main thread awaits critical zone");
            synchronized (this) {
                if (alerting != null){
                    alerting.finished = true;
                }
                logger.info("Main thread notifies global thread");
                this.notify();
            }
            logger.info("Main thread is out of critical zone");
        }

        public void add_thread(MyThread thread) {
            logger.info("Main thread awaits critical zone");
            synchronized (this) {
                logger.info("Added thread successfully to queue in " + this.getName());
                queue.add(thread);
                logger.info("Main thread notifies global thread");
                this.notify();
            }
            logger.info("Main thread is out of critical zone");
        }

        public void add_threads(List<MyThread> threads) {
            logger.info("Other thread awaits critical zone on " + this.getName());
            synchronized (this) {
                logger.warning("Other thread owns critical zone on " + this.getName());
                for (MyThread thr : threads) {
                    logger.info("Added thread successfully to queue in " + this.getName());
                    queue.add(thr);
                }
                logger.warning("Other thread notifies global thread " + this.getName());
                this.notify();
            }
            logger.info("Main thread is out of critical zone");
        }

        public void printProgress() {
            List<String> progress = new LinkedList<>();
            for (MyThread thread : this.active) {
                if (thread == null) {
                    progress.add("null");
                } else {
                    progress.add(thread.get_progress());
                }
            }
            System.out.println("ACTIVE: " + progress);
            System.out.println("QUEUE: " + queue.size());
        }
    }

    public static class FileQueueThread extends MyThread {
        final GlobalQueueThread file_queue_thread;
        final GlobalQueueThread download_queue_thread;
        String hash_to_download;
        final List<Manager> seed_managers;
        ByteSlice[] slices_array;
        final String file_location;

        public FileQueueThread(GlobalQueueThread file_thread, GlobalQueueThread download_thread, List<Manager> seed_managers, String hash_to_download, String file_location) {
            this.file_queue_thread = file_thread;
            this.download_queue_thread = download_thread;
            this.seed_managers = seed_managers;
            this.hash_to_download = hash_to_download;
            this.slices_array = null;
            this.file_location = file_location;
        }

        @Override
        public void run() {
            try {
                logger.warning("Attempting to add all threads");
                List<MyThread> threads = new LinkedList<>();
                for (int i = 0; i < this.slices_array.length; i++) {
                    Manager random_manager = seed_managers.get(new Random().nextInt(seed_managers.size()));
                    logger.info("Attempting to add " + this.hash_to_download + " " + i + "to download queue ");
                    threads.add(new DownloadThread(download_queue_thread, this, random_manager, i));
                }
                download_queue_thread.add_threads(threads);
                logger.warning("Threads added");
            } catch (Exception e) {
                logger.severe("ERROR WHILE DOWNLOADING " + hash_to_download);
            }
        }

        public void create_slice_array() throws Exception {
            this.slices_array = new ByteSlice[this.seed_managers.get(0).getSlicesNeeded(hash_to_download)];
            logger.warning("Created slice array");
        }

        public void write_file() {
            logger.warning("Starting to write " + this.hash_to_download);
            try (FileOutputStream stream = new FileOutputStream(file_location)) {
                for (ByteSlice byteSlice : this.slices_array) {
                    if (byteSlice == null) {
                        throw new RemoteException();
                    }
                    byte[] bytes = byteSlice.getBytes();
                    for (int j = 0; j < byteSlice.getBytes_written(); j++) {
                        stream.write(bytes[j]);
                    }
                }
            } catch (RemoteException e) {
                logger.severe("A download thread for " + hash_to_download + "failed");
            } catch (IOException e) {
                logger.severe("IOException while writing " + this.hash_to_download);
            }
            logger.severe("File " + hash_to_download + " downloaded!");
        }

        public boolean isFinished() {
            // NullPointerException aqui?????
            for (ByteSlice slice : slices_array) {
                if (slice == null) {
                    return false;
                }
            }
            return true;
        }

        public String get_progress() {
            int completed = 0;
            for (ByteSlice slice : slices_array) {
                if (slice != null) {
                    completed += 1;
                }
            }
            return hash_to_download + ":" + completed + "/" + slices_array.length;
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
            this.finished = false;
        }

        @Override
        public void run() {
            logger.info("Starting download thread " + this.file_thread.hash_to_download + " " + slice_index);
            try {
                ByteSlice result = seed_manager.get_slice(file_thread.hash_to_download, slice_index);
                file_thread.slices_array[slice_index] = result;
            } catch (Exception e) {
                file_thread.slices_array[slice_index] = null;
            }
            System.out.println("+++++++++++++++++++++");
            this.download_queue_thread.printProgress();
            System.out.println("---------------------");
            this.file_thread.file_queue_thread.printProgress();
            logger.info("Thread " + this.file_thread.hash_to_download + " " + slice_index + " is done!");
            this.file_thread.file_queue_thread.alert(this);
            this.download_queue_thread.alert(this);
            finished = true;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void write_file() {
        }

        @Override
        public void create_slice_array() {
        }

        @Override
        public String get_progress() {
            return file_thread.hash_to_download;
        }
    }
}


