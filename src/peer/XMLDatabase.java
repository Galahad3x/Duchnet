package peer;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class XMLDatabase {

    /**
     * Read XML info from a file
     * @param folder_route Route where the file is
     * @param extra_files List where to add the data
     * @return The list with the added data
     */
    public static List<Content> read_from_file(String folder_route, List<Content> extra_files) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(folder_route + "/content_info.xml"));

            doc.getDocumentElement().normalize();

            NodeList list = doc.getElementsByTagName("content");
            for (int temp = 0; temp < list.getLength(); temp++) {
                Node node = list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String hash = element.getAttribute("hash");

                    NodeList descriptions = element.getElementsByTagName("description");
                    NodeList tags = element.getElementsByTagName("tag");
                    for (Content content : extra_files) {
                        if (content.getHash().equals(hash)) {
                            for (int di = 0; di < descriptions.getLength(); di++) {
                                content.add_alternative_description(descriptions.item(di).getTextContent());
                            }
                            for (int di = 0; di < tags.getLength(); di++) {
                                content.add_tag(tags.item(di).getTextContent());
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            assert true;
        }
        return extra_files;
    }

    /**
     * Write XML info to content_info.xml
     * @param folder_route THe folder where to save it
     * @param files The files with the info we want to write
     */
    public static void write_to_xml(String folder_route, List<Content> files) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // Save files even if they aren't there anymore
        try {
            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

            Element rootEle = doc.createElement("contents");

            for (Content file : files) {
                Attr attribute = doc.createAttribute("hash");
                attribute.setValue(file.getHash());
                Element content = doc.createElement("content");
                content.setAttributeNode(attribute);
                for (String desc : file.getFileDescriptions()) {
                    Element descr = doc.createElement("description");
                    descr.appendChild(doc.createTextNode(desc));
                    content.appendChild(descr);
                }
                for (String desc : file.getTags()) {
                    Element descr = doc.createElement("tag");
                    descr.appendChild(doc.createTextNode(desc));
                    content.appendChild(descr);
                }
                rootEle.appendChild(content);
            }

            doc.appendChild(rootEle);
            try {
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                // send DOM to file
                tr.transform(new DOMSource(doc),
                        new StreamResult(new FileOutputStream(folder_route + "/content_info.xml")));

            } catch (TransformerException | IOException te) {
                System.out.println(te.getMessage());
            }

        } catch (ParserConfigurationException e) {
            assert true;
        }
    }
}
