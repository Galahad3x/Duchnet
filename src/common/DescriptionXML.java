package common;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

/**
 * Used to represent data in XML format
 */
public class DescriptionXML {
    public String hash;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> description;
    public Long id;

    public DescriptionXML(String hash, List<String> descriptions) {
        this.hash = hash;
        this.description = descriptions;
    }

    public DescriptionXML(String hash, Long id, List<String> descriptions) {
        this.hash = hash;
        this.description = descriptions;
        this.id = id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
