package common;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

/**
 * Used to represent data in XML format
 */
public class TagXML {
    public String hash;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> tag;

    public TagXML(String hash, List<String> tags) {
        this.hash = hash;
        this.tag = tags;
    }
}