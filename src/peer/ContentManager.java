package peer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ContentManager extends UnicastRemoteObject implements Remote, Manager {
    private final String folder_route;
    private final List<Content> contents;

    public ContentManager(String folder_route) throws RemoteException {
        super();
        this.folder_route = folder_route;
        this.contents = new ArrayList<>();
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
            Content this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                    Arrays.asList(descriptions),
                    this.getFileHash(file),
                    Arrays.asList(tags));
            extra_contents.add(this_file);
        }
        merge_lists(contents, extra_contents);
    }

    private List<Content> check_inside(File directory){
        List<Content> contents = new LinkedList<>();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile()) {
                Content this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                        new ArrayList<>(),
                        this.getFileHash(file),
                        new ArrayList<>());
                this_file.setLocal_route(file.getAbsolutePath());
                contents.add(this_file);
            } else {
                contents.addAll(check_inside(file));
            }
        }
        return contents;
    }

    /**
     * List all the files read in the folder
     * TODO Save the info on a file for later use (Quality Features: On a database)
     */
    public void list_files(boolean add_data) {
        // Just list local files
        if (!add_data) {
            File f = new File(this.folder_route);
            List<Content> extra_files = new LinkedList<>();
            for (File file : Objects.requireNonNull(f.listFiles())) {
                if (file.isFile()) {
                    Content this_file = new Content(new ArrayList<>(Collections.singleton(file.getName())),
                            new ArrayList<>(),
                            this.getFileHash(file),
                            new ArrayList<>());
                    this_file.setLocal_route(file.getAbsolutePath());
                    extra_files.add(this_file);
                } else {
                    extra_files.addAll(check_inside(file));
                }
            }
            // TODO Read hash related info in a file or db
            merge_lists(contents, extra_files);
        } else {
            update_files();
        }
    }

    public void print_contents(List<Content> contents) {
        for (Content content : contents) {
            System.out.println(content.getFilenames());
            System.out.println(content.getFileDescriptions());
            System.out.println(content.getTags());
            System.out.println(content.getHash());
            System.out.println("------------------------------");
        }
    }

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

    public List<Content> getContents() {
        return contents;
    }

    public String getFileHash(File file) {
        MessageDigest shaDigest = null;
        try {
            shaDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String shaChecksum = null;
        try {
            shaChecksum = getFileChecksum(shaDigest, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shaChecksum;
    }

    /**
     * Hash function from the Internet
     */
    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        //return complete hash
        return sb.toString();
    }

    @Override
    public Content download_file(String hash) throws Exception {
        Content to_download = null;
        for(Content file: this.getContents()){
            if(file.getHash().equals(hash)){
                to_download = file;
            }
        }
        if(to_download == null){
            throw new Exception("Hash not found");
        }
        to_download.setFile_data(Files.readAllBytes(Paths.get(to_download.getLocal_route())));
        return to_download;
    }

    public void add_content(Content downloaded_file) {
        this.contents.add(downloaded_file);
    }

    public String getFolder_route() {
        return folder_route;
    }
}
