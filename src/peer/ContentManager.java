package peer;

import common.ContentXML;
import common.DescriptionXML;
import common.TagXML;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Implementation of a Manager
 */
public class ContentManager extends UnicastRemoteObject implements Remote, Manager {
    /**
     * The route where we take files from and where we will save the ones we download
     */
    private final String folder_route;
    /**
     * List of all Contents, without file_data
     */
    private final List<Content> contents;
    /**
     * Semaphore to control uploading concurrency
     */
    private final Semaphore upload_semaphore;
    /**
     * The size of a slice that will be sent over the network, 1 MB (in bytes)
     */
    private final int slice_size = 1024 * 1024;
    /**
     * Own peerInfo for web services peers
     */
    private final PeerInfo info;

    /**
     * Cache hashmap to store likely-to-be-downloaded files
     */
    private final HashMap<String, ByteSlice[]> cache;

    /**
     * Logger used to print INFO, WARNINGS and SEVERES
     */
    private final Logger logger;

    /**
     * Service client used to call functions in the web service
     */
    private final ServiceClient serviceClient;

    /**
     * Constructor for ContentManager
     *
     * @param folder_route the route of the folder
     * @throws RemoteException when remote calls fail
     */
    public ContentManager(String folder_route, Semaphore upload_semaphore, Logger logger, PeerInfo info) throws RemoteException {
        super();
        this.folder_route = folder_route;
        this.contents = new ArrayList<>();
        this.upload_semaphore = upload_semaphore;
        cache = new HashMap<>();
        this.logger = logger;
        this.serviceClient = new ServiceClient(logger);
        this.info = info;
    }

    /**
     * Merge two content lists, merging same files into a single Content
     *
     * @param original The original list and the one returned
     * @param extra    The files to add to original
     */
    public static void merge_lists(List<Content> original, List<Content> extra) {
        for (Content this_file : extra) {
            boolean found = false;
            for (Content content : original) {
                if (content.getHash().equals(this_file.getHash())) {
                    found = true;
                    try {
                        if (!this_file.getFilenames().get(0).equals("")) {
                            for (String name : this_file.getFilenames()) {
                                if (!content.getFilenames().contains(name)) {
                                    content.add_alternative_name(name);
                                }
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        for (String name : this_file.getFilenames()) {
                            if (!content.getFilenames().contains(name)) {
                                content.add_alternative_name(name);
                            }
                        }
                    }
                    for (String desc : this_file.getFileDescriptions()) {
                        if (!desc.equals("") && !content.getFileDescriptions().contains(desc)) {
                            content.add_alternative_description(desc.strip());
                        }
                    }
                    for (String tag : this_file.getTags()) {
                        if (!tag.equals("") && !content.getTags().contains(tag)) {
                            content.add_tag(tag.strip());
                        }
                    }
                }
            }
            if (!found) {
                original.add(this_file);
            }
        }
    }

    public String getFolder_route() {
        return folder_route;
    }

    public List<Content> getContents() {
        databaseUpdate();
        return this.contents;
    }

    public void databaseUpdate() {
        try {
            serviceClient.deleteInfos(this.info);
        } catch (IOException | InterruptedException e) {
            logger.info("Request error");
        }
        for (Content content : contents) {
            if (content.getFilenames().size() > 0) {
                if (content.getFilenames().get(0).startsWith(".")) {
                    continue;
                }
            }
            try {
                for (String desc : content.getFileDescriptions()) {
                    serviceClient.postDescription(content.getHash(), desc);
                }
                for (String name : content.getFilenames()) {
                    serviceClient.postFilename(content.getHash(), name);
                }
                for (String tag : content.getTags()) {
                    serviceClient.postTag(content.getHash(), tag);
                }
                serviceClient.postPeer(content.getHash(), this.info);
            } catch (IOException | InterruptedException e) {
                logger.info("Request error");
            }
            try {
                ContentXML cXML = serviceClient.getEverything(content.getHash());
                if (cXML == null) {
                    continue;
                }
                if (cXML.description != null) {
                    for (String desc : cXML.description) {
                        content.add_alternative_description(desc);
                    }
                }
                if (cXML.filename != null) {
                    for (String name : cXML.filename) {
                        if (!content.getFilenames().contains(name)) {
                            content.add_alternative_name(name);
                        }
                    }
                }
                if (cXML.tag != null) {
                    for (String tag : cXML.tag) {
                        content.add_tag(tag);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.severe("Some error while processing XML");
                e.printStackTrace();
            }
        }
    }

    /**
     * List all the files in folder_route, or let the user add data
     *
     * @param add_data let the user add data or not
     */
    public void list_files(boolean add_data) {
        // Just list local files
        if (!add_data) {
            File f = new File(this.folder_route);
            List<Content> extra_files = new LinkedList<>();
            for (File file : Objects.requireNonNull(f.listFiles())) {
                if (file.isFile()) {
                    Content this_file = null;
                    try {
                        this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())), new ArrayList<>(), HashCalculator.getFileHash(file), new ArrayList<>());
                    } catch (IOException e) {
                        logger.severe("IOException while calculating file");
                    } finally {
                        assert this_file != null;
                        this_file.setLocal_route(file.getAbsolutePath());
                        extra_files.add(this_file);
                    }
                } else {
                    extra_files.addAll(check_inside(file, null));
                }
            }
            merge_lists(contents, extra_files);
            databaseUpdate();
        } else {
            update_files(this.folder_route);
        }
    }

    /**
     * Update files skipping the ones that don't satisfy the name restriction
     *
     * @param restriction the restriction to satisfy
     */
    public void list_filtered_files(String restriction) {
        File f = new File(this.folder_route);
        List<Content> extra_files = new LinkedList<>();
        FileFilter filter = file -> {
            String restriction_method = restriction.split(":")[0];
            String restriction_term;
            try {
                restriction_term = restriction.split(":")[1];
            } catch (IndexOutOfBoundsException e) {
                return true;
            }
            if (restriction_method.equals("name")) {
                return file.getName().toLowerCase().contains(restriction_term.toLowerCase()) || file.isDirectory();
            }
            return true;
        };
        for (File file : Objects.requireNonNull(f.listFiles(filter))) {
            if (file.isFile()) {
                Content this_file = null;
                try {
                    this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())), new ArrayList<>(), HashCalculator.getFileHash(file), new ArrayList<>());
                } catch (IOException e) {
                    logger.severe("IOException while calculating hash");
                } finally {
                    assert this_file != null;
                    this_file.setLocal_route(file.getAbsolutePath());
                    extra_files.add(this_file);
                }
            } else {
                extra_files.addAll(check_inside(file, filter));
            }
        }
        merge_lists(contents, extra_files);
        databaseUpdate();
    }

    /**
     * Filter a list of contents according to restriction
     *
     * @param contents    The original list of contents
     * @param restriction A restriction of the form name|description|tag:search_term
     * @return Contents that satisfy the condition
     */
    public List<Content> filter_contents(List<Content> contents, String restriction) {
        String[] restriction_terms = restriction.split(":");
        String search_method = restriction_terms[0].toLowerCase();
        String search_term;
        if (restriction_terms.length >= 2) {
            search_term = restriction_terms[1].toLowerCase();
        } else {
            return contents;
        }
        List<Content> new_contents = new LinkedList<>();
        for (Content content : contents) {
            List<String> list_to_check;
            switch (search_method) {
                case "description":
                    list_to_check = content.getFileDescriptions();
                    break;
                case "tag":
                    list_to_check = content.getTags();
                    break;
                default:
                    list_to_check = content.getFilenames();
            }
            for (String term : list_to_check) {
                if (term.toLowerCase().startsWith(search_term.toLowerCase())) {
                    new_contents.add(content);
                }
            }
        }
        for (Content content : contents) {
            List<String> list_to_check;
            switch (search_method) {
                case "description":
                    list_to_check = content.getFileDescriptions();
                    break;
                case "tag":
                    list_to_check = content.getTags();
                    break;
                default:
                    list_to_check = content.getFilenames();
            }
            for (String term : list_to_check) {
                if (term.toLowerCase().contains(search_term.toLowerCase()) && !new_contents.contains(content)) {
                    new_contents.add(content);
                }
            }
        }
        return new_contents;
    }

    /**
     * List all the files inside directories found inside the folder_route recursively
     *
     * @param directory the File of the directory
     * @return List of all contents in the directory, including inside other directories
     */
    private List<Content> check_inside(File directory, FileFilter filter) {
        List<Content> contents = new LinkedList<>();
        if (filter == null) filter = file -> true;
        for (File file : Objects.requireNonNull(directory.listFiles(filter))) {
            if (file.isFile()) {
                Content this_file = null;
                try {
                    this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())), new ArrayList<>(), HashCalculator.getFileHash(file), new ArrayList<>());
                } catch (IOException e) {
                    logger.severe("IOException while calculating hash");
                } finally {
                    assert this_file != null;
                    this_file.setLocal_route(file.getAbsolutePath());
                    contents.add(this_file);
                }
            } else {
                contents.addAll(check_inside(file, null));
            }
        }
        return contents;
    }

    /**
     * List all the files in the folder while letting the user add descriptions and tags
     */
    private void update_files(String route) {
        File f = new File(route);
        List<Content> extra_contents = new LinkedList<>();
        for (File file : Objects.requireNonNull(f.listFiles(file -> !file.getName().startsWith(".")))) {
            if (file.isDirectory()) {
                update_files(file.getAbsolutePath());
                continue;
            }
            System.out.println("File name: " + file.getName());
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type descriptions separated by , or leave blank: ");
            String[] descriptions = scanner.nextLine().split(",");
            System.out.println("Type tags separated by , or leave blank: ");
            String[] tags = scanner.nextLine().split(",");
            Content this_file = null;
            try {
                this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())), new LinkedList<>(Arrays.asList(descriptions)), HashCalculator.getFileHash(file), new LinkedList<>(Arrays.asList(tags)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            extra_contents.add(this_file);
        }
        merge_lists(contents, extra_contents);
        databaseUpdate();
    }

    /**
     * Print a list of contents
     *
     * @param contents the list to print
     */
    public void print_contents(List<Content> contents) {
        for (Content content : contents) {
            boolean to_skip = false;
            for (String filename : content.getFilenames()) {
                if (filename.startsWith(".")) {
                    to_skip = true;
                    break;
                }
            }
            if (to_skip) {
                continue;
            }
            System.out.println(content.getFilenames());
            System.out.println(content.getFileDescriptions());
            System.out.println(content.getTags());
            System.out.println(content.getHash());
            System.out.println("------------------------------");
        }
    }

    /**
     * Given a hash, return the Content with a file_data, called remotely
     *
     * @param hash the hash of the file to return
     * @return a Content with its file_data
     * @throws Exception if something fails
     */
    @Override
    public ByteSlice get_slice(String hash, Integer slice_index) throws Exception {
        Content to_download = null;
        for (Content file : this.getContents()) {
            if (file.getHash().equals(hash)) {
                to_download = file;
            }
        }
        if (to_download == null) {
            throw new Exception("Hash not found");
        }
        logger.info("Received new download thread");
        this.upload_semaphore.acquire();
        File file = new File(to_download.getLocal_route());
        byte[] bytes;
        synchronized (cache) {
            if (cache.containsKey(hash) && cache.get(hash)[slice_index] != null) {
                this.upload_semaphore.release();
                logger.info("Freed download thread");
                return cache.get(hash)[slice_index];
            }
        }
        ByteSlice[] the_file = new ByteSlice[this.getSlicesNeeded(hash)];
        try (FileInputStream stream = new FileInputStream(file)) {
            for (int i = 0; i < this.getSlicesNeeded(hash); i++) {
                bytes = new byte[slice_size];
                int j;
                for (j = 0; j < slice_size; j++) {
                    int the_byte = stream.read();
                    if (the_byte >= 0) {
                        bytes[j] = (byte) the_byte;
                    } else {
                        break;
                    }
                }
                the_file[i] = new ByteSlice(bytes, j);
            }
        }
        synchronized (cache) {
            if (cache.size() >= 4) {
                logger.info("Removing file from cache");
                cache.remove((String) cache.keySet().toArray()[0]);
            }
            if (!cache.containsKey(hash)) {
                logger.info("Adding file to cache");
                cache.put(hash, the_file);
            }
            this.upload_semaphore.release();
            logger.info("Freed download thread");
            return cache.get(hash)[slice_index];
        }
    }

    /**
     * Request a content's information from a seed
     *
     * @param hash The hash of the file we want
     * @return The content, with the descriptions and the tags
     * @throws Exception If the remote connection fails
     */
    @Override
    public Content get_information(String hash) throws Exception {
        Content to_download = null;
        for (Content file : this.getContents()) {
            if (file.getHash().equals(hash)) {
                to_download = file;
            }
        }
        if (to_download == null) {
            throw new Exception("Hash not found");
        }
        return to_download;
    }

    /**
     * Return the name of a file as it is in the folder
     *
     * @param hash  The hash of the file
     * @param names The possible names this file has
     * @return The name if found, null if not
     * @throws Exception If the remote connection or IO fails
     */
    @Override
    public String get_filename(String hash, List<String> names) throws Exception {
        for (String name : names) {
            for (File f : Objects.requireNonNull(new File(this.folder_route).listFiles(fi -> fi.getName().endsWith(name)))) {
                if (hash.equals(HashCalculator.getFileHash(f))) {
                    return f.getName();
                }
            }
        }
        return null;
    }

    /**
     * Return the number of slices of size 1 MB needed to get the whole file
     *
     * @param hash The hash of the file we want to slice
     * @return The necessary number of slices
     * @throws Exception If something fails
     */
    @Override
    public Integer getSlicesNeeded(String hash) throws Exception {
        Content to_download = null;
        for (Content file : this.getContents()) {
            if (file.getHash().equals(hash)) {
                to_download = file;
            }
        }
        if (to_download == null) {
            throw new Exception("Hash not found");
        }
        File file = new File(to_download.getLocal_route());
        return ((int) Math.ceil(file.length() / (float) slice_size));
    }

    /**
     * When downloading a file, this confirms the downloader if the file is whole or if it will be downloaded by chunks
     * If separating in chunks is necessary, it is done and all hashes returned
     *
     * @param hash The hash of the whole file
     * @return Hashes needed to download the whole file
     * @throws Exception If something fails
     */
    @Override
    public List<String> getHashesNeeded(String hash) throws Exception {
        Content to_download = null;
        for (Content file : this.getContents()) {
            if (file.getHash().equals(hash)) {
                to_download = file;
            }
        }
        if (to_download == null) {
            throw new Exception("Hash not found");
        }
        File file = new File(to_download.getLocal_route());
        String name = file.getName();
        if ((file.length() / slice_size) > 25) {
            FileSlicer.splitFile(file);
        } else {
            return new LinkedList<>(Collections.singleton(hash));
        }
        List<String> hashes = new LinkedList<>();
        this.list_filtered_files("name:" + name);
        for (File f : Objects.requireNonNull(new File(this.folder_route).listFiles(fi -> fi.getName().contains(name) && !fi.getName().startsWith(name)))) {
            for (Content elem : this.getContents()) {
                if (elem.getFilenames().contains(f.getName())) {
                    hashes.add(elem.getHash());
                }
            }
        }
        return hashes;
    }

    /**
     * Get seeders of a hash from the web server
     *
     * @param hash Hash of the file
     * @return List of seeders
     */
    public List<PeerInfo> getSeeders(String hash) {
        try {
            return this.serviceClient.getSeeders(hash);
        } catch (IOException | InterruptedException e) {
            return new LinkedList<>();
        }
    }

    /**
     * Register a user in the web service
     *
     * @param username Username
     * @param password Password
     * @return True if success
     */
    public boolean register(String username, String password) throws IOException, InterruptedException {
        this.login(username, password);
        return this.serviceClient.register();
    }

    /**
     * Add the login info to the service client, so it can be used in the requests
     *
     * @param username username
     * @param password password
     */
    public void login(String username, String password) {
        serviceClient.username = username;
        serviceClient.password = password;
    }

    public boolean change_password(String pssword) throws IOException, InterruptedException {
        return serviceClient.changePassword(pssword);
    }

    public void delete(String hash) {
        if (hash.equals("")) {
            try {
                serviceClient.deleteDescriptions();
                serviceClient.deleteFilenames();
                serviceClient.deleteTags();
                serviceClient.deleteInfos(this.info);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (hash.equals("cancel")) {
            return;
        } else {
            try {
                serviceClient.deleteContent(hash);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("Deleted");
    }

    public void deleteInfos() {
        try {
            serviceClient.deleteInfos(this.info);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<Content> getServiceContents() {
        List<ContentXML> cXMLs = null;
        try {
            cXMLs = serviceClient.getEverything();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        List<Content> contents = new LinkedList<>();
        if (cXMLs == null) {
            return contents;
        }
        for (ContentXML cXML : cXMLs) {
            Content content = new Content(new LinkedList<>(), new LinkedList<>(), cXML.hash, new LinkedList<>());
            if (cXML.description != null) {
                for (String desc : cXML.description) {
                    content.add_alternative_description(desc);
                }
            }
            if (cXML.filename != null) {
                for (String name : cXML.filename) {
                    if (!content.getFilenames().contains(name)) {
                        content.add_alternative_name(name);
                    }
                }
            }
            if (cXML.tag != null) {
                for (String tag : cXML.tag) {
                    content.add_tag(tag);
                }
            }
            contents.add(content);
        }
        databaseUpdate();
        return contents;
    }

    public void wsModify(String type) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        if (type.equals("descriptions")) {
            List<DescriptionXML> XMLs = this.serviceClient.getDescriptions();
            if (XMLs == null) {
                return;
            }
            for (DescriptionXML XML : XMLs) {
                System.out.println(XML.hash + " (" + XML.id + "): " + XML.description.get(0));
            }
            System.out.println("Select ID: ");
            long id;
            try {
                id = Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                logger.warning("Error while parsing ID");
                return;
            }
            System.out.println("Type new description: ");
            String new_desc = scanner.nextLine();
            if (this.serviceClient.modifyDescription(id, new_desc)){
                boolean found = false;
                for (Content content : contents) {
                    if (content.getFileDescriptions() != null) {
                        if (found) {
                            break;
                        }
                        for (String desc : content.getFileDescriptions()) {
                            if (desc.equals(new_desc)) {
                                found = true;
                                content.getFileDescriptions().remove(desc);
                                break;
                            }
                        }
                    }
                }
                databaseUpdate();
            }
        } else if (type.equals("tags")) {
            List<TagXML> XMLs = this.serviceClient.getTags();
            if (XMLs == null) {
                return;
            }
            for (TagXML XML : XMLs) {
                System.out.println(XML.hash + " (" + XML.id + "): " + XML.tag.get(0));
            }
            System.out.println("Select ID: ");
            long id;
            try {
                id = Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                logger.warning("Error while parsing ID");
                return;
            }
            System.out.println("Type new tag: ");
            String new_desc = scanner.nextLine();
            if (this.serviceClient.modifyTag(id, new_desc)) {
                boolean found = false;
                for (Content content : contents) {
                    if (content.getTags() != null) {
                        if (found) {
                            break;
                        }
                        for (String desc : content.getTags()) {
                            if (desc.equals(new_desc)) {
                                found = true;
                                content.getTags().remove(desc);
                                break;
                            }
                        }
                    }
                }
                databaseUpdate();
            }
        } else {
            logger.warning("Resource type not valid");
        }
    }

    public void wsDelete(String type) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        if (type.equals("descriptions")) {
            List<DescriptionXML> XMLs = this.serviceClient.getDescriptions();
            if (XMLs == null) {
                return;
            }
            for (DescriptionXML XML : XMLs) {
                System.out.println(XML.hash + " (" + XML.id + "): " + XML.description.get(0));
            }
            System.out.println("Select ID: ");
            Long id;
            try {
                id = Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                logger.warning("Error while parsing ID");
                return;
            }
            String new_desc = null;
            for (DescriptionXML XML : XMLs) {
                if (id.equals(XML.id)) {
                    new_desc = XML.description.get(0);
                    break;
                }
            }
            if (this.serviceClient.deleteDescription(id)){
                boolean found = false;
                for (Content content : contents) {
                    if (content.getFileDescriptions() != null) {
                        if (found) {
                            break;
                        }
                        for (String desc : content.getFileDescriptions()) {
                            if (desc.equals(new_desc)) {
                                found = true;
                                content.getFileDescriptions().remove(desc);
                                break;
                            }
                        }
                    }
                }
                databaseUpdate();
            }
        } else if (type.equals("tags")) {
            List<TagXML> XMLs = this.serviceClient.getTags();
            if (XMLs == null) {
                return;
            }
            for (TagXML XML : XMLs) {
                System.out.println(XML.hash + " (" + XML.id + "): " + XML.tag.get(0));
            }
            System.out.println("Select ID: ");
            Long id;
            try {
                id = Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                logger.warning("Error while parsing ID");
                return;
            }
            String new_desc = null;
            for (TagXML XML : XMLs) {
                if (id.equals(XML.id)) {
                    new_desc = XML.tag.get(0);
                    break;
                }
            }
            if (this.serviceClient.deleteTag(id)){
                boolean found = false;
                for (Content content : contents) {
                    if (content.getTags() != null) {
                        if (found) {
                            break;
                        }
                        for (String desc : content.getTags()) {
                            if (desc.equals(new_desc)) {
                                found = true;
                                content.getTags().remove(desc);
                                break;
                            }
                        }
                    }
                }
                databaseUpdate();
            }
        } else {
            logger.warning("Resource type not valid");
        }
    }
}
