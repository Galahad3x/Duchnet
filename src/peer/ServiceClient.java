package peer;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import common.ContentXML;
import common.DescriptionXML;
import common.FilenameXML;
import common.TagXML;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class ServiceClient {

    private final Logger logger;

    private final String protocol = "http://";
    private final String baseurl = protocol + "localhost:8080" + "/v3";
    public String username;
    public String password;

    public ServiceClient(Logger logger) {
        this.logger = logger;
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.SEVERE);
        Logger.getLogger("httpclient").setLevel(Level.SEVERE);
    }

    public List<ContentXML> getEverything() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseurl + "/"))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<ContentXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            List<String> fname;
            List<String> descs;
            List<String> tgs;
            try {
                fname = (List<String>) hmap.get("filename");
            } catch (ClassCastException e) {
                fname = new LinkedList<>(Collections.singletonList((String) hmap.get("filename")));
            }
            try {
                descs = (List<String>) hmap.get("description");
            } catch (ClassCastException e) {
                descs = new LinkedList<>(Collections.singletonList((String) hmap.get("description")));
            }
            try {
                tgs = (List<String>) hmap.get("tag");
            } catch (ClassCastException e) {
                tgs = new LinkedList<>(Collections.singletonList((String) hmap.get("tag")));
            }
            ContentXML content = new ContentXML((String) hmap.get("hash"), fname, descs, tgs);
            retval.add(content);
        }
        return retval;
    }

    public ContentXML getEverything(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseurl + String.format("/contents/%s", hash)))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        String body = response.body();
        return new XmlMapper().readValue(body, ContentXML.class);
    }


    public List<FilenameXML> getFilenames() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "filenames")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<FilenameXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new FilenameXML((String) hmap.get("hash"), (List<String>) hmap.get("filename")));
        }
        return retval;
    }

    public List<FilenameXML> getFilenames(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "filenames")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<FilenameXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new FilenameXML((String) hmap.get("hash"), (List<String>) hmap.get("filename")));
        }
        return retval;
    }

    public void deleteFilenames() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "filenames")))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
        }
    }

    public void postFilename(String hash, String text) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "filenames")))
                .method("PUT", HttpRequest.BodyPublishers.ofString(text))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response1 = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response1.statusCode() != 201) {
            logger.info("Request to web server failed " + response1.statusCode());
            return;
        }
        if (response1.statusCode() == 404) {
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "filenames")))
                    .method("POST", HttpRequest.BodyPublishers.ofString(text))
                    .header("Content-Type", "text/plain")
                    .header("username", this.username)
                    .header("password", this.password)
                    .build();
            HttpResponse<String> response2 = client.send(request2, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response2.statusCode() != 201) {
                logger.info("Request to web server failed " + response2.statusCode());
            }
        }
        logger.info("Request to web server successful");
    }


    public List<DescriptionXML> getDescriptions() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "descriptions")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<DescriptionXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new DescriptionXML((String) hmap.get("hash"), Long.parseLong((String) hmap.get("id")), new LinkedList<>(Collections.singletonList((String) hmap.get("description")))));
        }
        return retval;
    }

    public List<DescriptionXML> getDescriptions(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "descriptions")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<DescriptionXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new DescriptionXML((String) hmap.get("hash"), (List<String>) hmap.get("description")));
        }
        return retval;
    }

    public void deleteDescriptions() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "descriptions")))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return;
        }
        logger.info("Request to web server successful");
    }

    public void postDescription(String hash, String text) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "descriptions")))
                .method("PUT", HttpRequest.BodyPublishers.ofString(text))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response1 = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response1.statusCode() != 201) {
            logger.info("Request to web server failed " + response1.statusCode());
            return;
        }
        if (response1.statusCode() == 404) {
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "descriptions")))
                    .method("POST", HttpRequest.BodyPublishers.ofString(text))
                    .header("Content-Type", "text/plain")
                    .header("username", this.username)
                    .header("password", this.password)
                    .build();
            HttpResponse<String> response2 = client.send(request2, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response2.statusCode() != 201) {
                logger.info("Request to web server failed " + response2.statusCode());
            }
        }
        logger.info("Request to web server successful");
    }

    public boolean modifyDescription(Long id, String desc) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s/%s", baseurl, "descriptions", id.toString())))
                .method("PUT", HttpRequest.BodyPublishers.ofString(desc))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed");
            return false;
        } else {
            logger.info("Request to web server successful");
            return true;
        }
    }


    public List<TagXML> getTags() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "tags")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<TagXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new TagXML((String) hmap.get("hash"), Long.parseLong((String) hmap.get("id")), new LinkedList<>(Collections.singletonList((String) hmap.get("tag")))));
        }
        return retval;
    }

    public List<TagXML> getTags(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "tags")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<TagXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new TagXML((String) hmap.get("hash"), (List<String>) hmap.get("tag")));
        }
        return retval;
    }

    public void deleteTags() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s", baseurl, "tags")))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return;
        }
        logger.info("Request to web server successful");
    }

    public void postTag(String hash, String text) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "tags")))
                .method("PUT", HttpRequest.BodyPublishers.ofString(text))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response1 = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response1.statusCode() != 201) {
            logger.info("Request to web server failed " + response1.statusCode());
            return;
        }
        if (response1.statusCode() == 404) {
            HttpRequest request2 = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "tags")))
                    .method("POST", HttpRequest.BodyPublishers.ofString(text))
                    .header("Content-Type", "text/plain")
                    .header("username", this.username)
                    .header("password", this.password)
                    .build();
            HttpResponse<String> response2 = client.send(request2, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response2.statusCode() != 201) {
                logger.info("Request to web server failed " + response2.statusCode());
            }
        }
        logger.info("Request to web server successful");
    }

    public boolean modifyTag(Long id, String desc) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s/%s", baseurl, "tags", id.toString())))
                .method("PUT", HttpRequest.BodyPublishers.ofString(desc))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed");
            return false;
        } else {
            logger.info("Request to web server successful");
            return true;
        }
    }


    public List<PeerInfo> getSeeders(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "peers")))
                .method("GET", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.body(), List.class);
        List<PeerInfo> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(PeerInfo.fromString((String) hmap.get("item")));
        }
        return retval;
    }

    public void deleteInfos(PeerInfo info) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/search/%s", baseurl, "peers")))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(info.toString()))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return;
        }
        logger.info("Request to web server successful");
    }

    public void postPeer(String hash, PeerInfo info) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s/%s", baseurl, hash, "peers")))
                .method("POST", HttpRequest.BodyPublishers.ofString(info.toString()))
                .header("Content-Type", "text/plain")
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            logger.info("Request to web server failed " + response.statusCode());
            return;
        }
        logger.info("Request to web server successful");
    }

    public boolean register() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/auth", baseurl)))
                .method("POST", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            logger.info("Request to web server failed " + response.statusCode());
            return false;
        }
        logger.info("Request to web server successful");
        return true;
    }

    public boolean changePassword(String pssword) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/auth", baseurl)))
                .method("PUT", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .header("new_password", pssword)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return false;
        }
        logger.info("Request to web server successful");
        return true;
    }

    public void deleteContent(String hash) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/contents/%s", baseurl, hash)))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return;
        }
        logger.info("Request to web server successful");
    }

    public boolean deleteDescription(Long id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s/%s", baseurl, "descriptions", id.toString())))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return false;
        }
        logger.info("Request to web server successful");
        return true;
    }

    public boolean deleteTag(Long id) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/resources/%s/%s", baseurl, "tags", id.toString())))
                .method("DELETE", HttpRequest.BodyPublishers.ofString(""))
                .header("username", this.username)
                .header("password", this.password)
                .build();
        HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            logger.info("Request to web server failed " + response.statusCode());
            return false;
        }
        logger.info("Request to web server successful");
        return true;
    }
}
