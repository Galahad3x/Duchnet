package peer;

import java.io.*;
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
     * Cache hashmap to store likely-to-be-downloaded files
     */
    private final HashMap<String, ByteSlice[]> cache;

    /**
     * Logger used to print INFO, WARNINGS and SEVERES
     */
    private final Logger logger;

    /**
     * Constructor for ContentManager
     *
     * @param folder_route the route of the folder
     * @throws RemoteException when remote calls fail
     */
    public ContentManager(String folder_route, Semaphore upload_semaphore, Logger logger) throws RemoteException {
        super();
        this.folder_route = folder_route;
        this.contents = new ArrayList<>();
        this.upload_semaphore = upload_semaphore;
        cache = new HashMap<>();
        this.logger = logger;
    }

    public String getFolder_route() {
        return folder_route;
    }

    public List<Content> getContents() {
        return XMLDatabase.read_from_file(this.folder_route, contents);
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
                        this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                                new ArrayList<>(),
                                HashCalculator.getFileHash(file),
                                new ArrayList<>());
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
            XMLDatabase.read_from_file(this.folder_route, extra_files);
            XMLDatabase.write_to_xml(this.folder_route, extra_files);
            merge_lists(contents, extra_files);
        } else {
            update_files();
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
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
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
            }
        };
        for (File file : Objects.requireNonNull(f.listFiles(filter))) {
            if (file.isFile()) {
                Content this_file = null;
                try {
                    this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                            new ArrayList<>(),
                            HashCalculator.getFileHash(file),
                            new ArrayList<>());
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
        XMLDatabase.read_from_file(this.folder_route, contents);
        XMLDatabase.write_to_xml(this.folder_route, contents);
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
                    this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                            new ArrayList<>(),
                            HashCalculator.getFileHash(file),
                            new ArrayList<>());
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
    private void update_files() {
        File f = new File(this.folder_route);
        List<Content> extra_contents = new LinkedList<>();
        // TODO add directory support to this function
        for (File file : Objects.requireNonNull(f.listFiles(File::isFile))) {
            System.out.println("File name: " + file.getName());
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type descriptions separated by , or leave blank: ");
            String[] descriptions = scanner.nextLine().split(",");
            System.out.println("Type tags separated by , or leave blank: ");
            String[] tags = scanner.nextLine().split(",");
            Content this_file = null;
            try {
                this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                        new LinkedList<>(Arrays.asList(descriptions)),
                        HashCalculator.getFileHash(file),
                        new LinkedList<>(Arrays.asList(tags)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            extra_contents.add(this_file);
        }
        XMLDatabase.read_from_file(this.folder_route, extra_contents);
        merge_lists(contents, extra_contents);
        XMLDatabase.write_to_xml(this.folder_route, contents);
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
            if (to_skip){
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
                    if (!this_file.getFilenames().get(0).equals("")) {
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
            for (File f : Objects.requireNonNull(new File(this.folder_route).listFiles(fi -> fi.getName().endsWith(name) && !fi.getName().startsWith(name)))) {
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
}
