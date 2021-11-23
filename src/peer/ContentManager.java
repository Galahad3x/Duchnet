package peer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class ContentManager extends UnicastRemoteObject implements Remote, Manager {
    /**
     * The route where we take files from and where we will save the ones we download
     */
    private final String folder_route;
    /**
     * List of all Contents, without file_data
     */
    private final List<Content> contents;
    private final int slice_size = 1000;

    /**
     * Constructor for ContentManager
     *
     * @param folder_route the route of the folder
     * @throws RemoteException when remote calls fail
     */
    public ContentManager(String folder_route) throws RemoteException {
        super();
        this.folder_route = folder_route;
        this.contents = new ArrayList<>();
    }

    public String getFolder_route() {
        return folder_route;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void add_content(Content downloaded_file) {
        this.contents.add(downloaded_file);
    }

    /**
     * List all the files in folder_route, or let the user add data
     * TODO Save the info on a file for later use (Quality Features: On a database)
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    assert this_file != null;
                    this_file.setLocal_route(file.getAbsolutePath());
                    extra_files.add(this_file);
                } else {
                    extra_files.addAll(check_inside(file, null));
                }
            }
            // TODO Read hash related info in a file or db
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
        FileFilter filter = file -> {
            String restriction_method = restriction.split(":")[0];
            String restriction_term;
            try {
                restriction_term = restriction.split(":")[1];
            } catch (IndexOutOfBoundsException e){
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
                    this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                            new ArrayList<>(),
                            HashCalculator.getFileHash(file),
                            new ArrayList<>());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assert this_file != null;
                this_file.setLocal_route(file.getAbsolutePath());
                extra_files.add(this_file);
            } else {
                extra_files.addAll(check_inside(file, filter));
            }
        }
        // TODO Read hash related info in a file or db
        merge_lists(contents, extra_files);
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
                assert this_file != null;
                this_file.setLocal_route(file.getAbsolutePath());
                contents.add(this_file);
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
        for (File file : Objects.requireNonNull(f.listFiles())) {
            System.out.println("File name: " + file.getName());
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type descriptions separated by , or leave blank: ");
            String[] descriptions = scanner.nextLine().split(",");
            System.out.println("Type tags separated by , or leave blank: ");
            String[] tags = scanner.nextLine().split(",");
            Content this_file = null;
            try {
                this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                        Arrays.asList(descriptions),
                        HashCalculator.getFileHash(file),
                        Arrays.asList(tags));
            } catch (Exception e) {
                e.printStackTrace();
            }
            extra_contents.add(this_file);
        }
        merge_lists(contents, extra_contents);
    }

    /**
     * Print a list of contents
     *
     * @param contents the list to print
     */
    public void print_contents(List<Content> contents) {
        for (Content content : contents) {
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
    // TODO Parallelize download slices
    @Override
    public byte[] get_slice(String hash, Integer slice_index) throws Exception {
        System.out.println("Downloading " + hash + " slice " + slice_index);
        System.out.print(Runtime.getRuntime().maxMemory());
        System.out.print(" ");
        System.out.println(Runtime.getRuntime().freeMemory());
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
        byte[] bytes;
        synchronized (this) {
            bytes = new byte[slice_size];
            try (FileInputStream fis = new FileInputStream(file)) {
                try {
                    int read = fis.read(bytes, slice_index * slice_size, slice_size);
                } catch (IndexOutOfBoundsException e) {
                    bytes = fis.readNBytes((int) file.length() - slice_index * slice_size);
                }
            }
        }
        return bytes;
    }

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
        return ((int) Math.ceil(file.length() / (slice_size)));
    }
}
