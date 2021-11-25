package peer;

import java.io.Serializable;
import java.util.List;

public class Content implements Serializable {
    /**
     * The bytes of the file, only used when sending the file to a Peer
     */
    private byte[] file_data;
    /**
     * The filenames the file has, in the same host or on another
     */
    private final List<String> file_name;
    /**
     * Route where the file is, including subdirectories
     */
    private String local_route;
    /**
     * The descriptions the file has, in the same host or on another
     */
    private final List<String> file_description;
    /**
     * The CRC32 hash of the file
     */
    private final String hash;
    /**
     * The tags the file has, in the same host or on another
     */
    private final List<String> tags;

    /**
     * Constructor used to create a Content
     * @param file_names the filenames the file has
     * @param file_description the possible the file has
     * @param hash the hash of the file
     * @param tags the tags the file has
     */
    public Content(List<String> file_names, List<String> file_description, String hash, List<String> tags) {
        this.file_name = file_names;
        this.file_description = file_description;
        this.hash = hash;
        this.tags = tags;
    }

    public List<String> getFilenames() {
        return file_name;
    }
    public void add_alternative_name(String name) {
        file_name.add(name);
    }

    public List<String> getFileDescriptions() {
        return file_description;
    }
    public void add_alternative_description(String description) {
        file_description.add(description);
    }

    public List<String> getTags() {
        return tags;
    }
    public void add_tag(String tag) {
        tags.add(tag);
    }

    public String getHash() {
        return hash;
    }

    public String getLocal_route() {
        return local_route;
    }
    public void setLocal_route(String local_route) {
        this.local_route = local_route;
    }

    public void setFile_data(byte[] file_data) {
        this.file_data = file_data;
    }
    public byte[] getFile_data() {
        return file_data;
    }
}
