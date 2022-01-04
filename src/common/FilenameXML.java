package common;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

/**
 * Used to represent data in XML format
 */
public class FilenameXML {
    public String hash;
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> filename;

    public FilenameXML(String hash, List<String> filenames) {
        this.hash = hash;
        this.filename = filenames;
    }
}