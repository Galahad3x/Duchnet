package common;

import java.util.List;

public class DescriptionXML {
    public String hash;
    public List<String> descriptions;

    public DescriptionXML(String hash, List<String> descriptions) {
        this.hash = hash;
        this.descriptions = descriptions;
    }
}