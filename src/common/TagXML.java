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
    public Long id;

    public TagXML(String hash, List<String> tags) {
        this.hash = hash;
        this.tag = tags;
    }

    public TagXML(String hash, Long id, List<String> tags) {
        this.hash = hash;
        this.tag = tags;
        this.id = id;
    }

    public TagXML(){

    }

    public void setId(Long id) {
        this.id = id;
    }
}