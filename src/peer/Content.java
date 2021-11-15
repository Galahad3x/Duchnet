package peer;

import java.io.Serializable;
import java.util.List;

public class Content implements Serializable {
    private byte[] file_data;
    private final List<String> file_name;
    private String local_route;
    private final List<String> file_description;
    private final String hash;
    private final List<String> tags;

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

    public void add_tag(String tag) {
        tags.add(tag);
    }

    public String getHash() {
        return hash;
    }

    public List<String> getTags() {
        return tags;
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
