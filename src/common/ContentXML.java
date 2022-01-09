package common;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

/**
 * Used to represent data in XML format
 */
public class ContentXML {
    public String hash;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> filename;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> description;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> tag;

    public ContentXML(String hash, List<String> filenames, List<String> descriptions, List<String> tags) {
        this.hash = hash;
        this.filename = filenames;
        this.description = descriptions;
        this.tag = tags;
    }

    public ContentXML() {

    }
}
