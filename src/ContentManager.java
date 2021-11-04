import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ContentManager extends UnicastRemoteObject implements Remote {
    private String folder_route = "";
    private List<Content> contents;

    public ContentManager(String folder_route) throws RemoteException {
        super();
        this.folder_route = folder_route;
        this.contents = new ArrayList<>();
    }

    private void update_files(boolean user_intervention){
        if(!user_intervention){
            File f = new File(this.folder_route);
            for (File file : Objects.requireNonNull(f.listFiles())){
                Content this_file = new Content(new ArrayList<String>(Collections.singleton(file.getName())),
                        new ArrayList<>(Collections.singleton("")),
                        this.getFileHash(file),
                        new ArrayList<String>(Collections.singleton("")));
                boolean found = false;
                for(Content content : contents){
                    if (content.getHash().equals(this_file.getHash())){
                        found = true;
                        if (!this_file.getFilenames().get(0).equals("")) {
                            content.add_alternative_name(this_file.getFilenames().get(0));
                        }
                        content.add_alternative_description(this_file.getFileDescriptions().get(0));
                    }
                }
                if(!found){
                    contents.add(this_file);
                }
            }
            for (Content content : contents){
                System.out.println(content.getFilenames());
                System.out.println(content.getFileDescriptions());
                System.out.println(content.getTags());
                System.out.println(content.getHash());
                System.out.println("------------------------------");
            }
        }
    }
    /*
        List local files
     */
    public void list_files(){

    }

    public String getFileHash(File file){
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
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

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
}
