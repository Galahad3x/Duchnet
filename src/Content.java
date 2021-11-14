import java.util.List;

public class Content {
    private byte[] file_data;
    private final List<String> file_name;
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

    public byte[] getFiledata() {
        return file_data;
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
}
