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

    public DescriptionXML(String hash, List<String> descriptions) {
        this.hash = hash;
        this.description = descriptions;
    }
}
