package peer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import common.ContentXML;
import common.DescriptionXML;
import common.FilenameXML;
import common.TagXML;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class ServiceClient {

    private final Logger logger;

    public ServiceClient(Logger logger) {
        this.logger = logger;
    }

    public List<ContentXML> getEverything() throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed " + response.getStatusText());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<ContentXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            ContentXML content = new ContentXML((String) hmap.get("hash"), (List<String>) hmap.get("filename"), (List<String>) hmap.get("description"), (List<String>) hmap.get("tag"));
            retval.add(content);
        }
        return retval;
    }

    public List<ContentXML> getEverything(String hash) throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/contents/{hash}")
                .routeParam("hash", hash)
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed " + response.getStatusText());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<ContentXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            ContentXML content = new ContentXML((String) hmap.get("hash"), (List<String>) hmap.get("filename"), (List<String>) hmap.get("description"), (List<String>) hmap.get("tag"));
            retval.add(content);
        }
        return retval;
    }


    public List<FilenameXML> getFilenames() throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "filenames")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed " + response.getStatusText());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<FilenameXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new FilenameXML((String) hmap.get("hash"), (List<String>) hmap.get("filename")));
        }
        return retval;
    }

    public List<FilenameXML> getFilenames(String hash) throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "filenames")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed " + response.getStatusText());
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<FilenameXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new FilenameXML((String) hmap.get("hash"), (List<String>) hmap.get("filename")));
        }
        return retval;
    }

    public void deleteFilenames() throws UnirestException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.DELETE,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "filenames")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed " + response.getStatusText());
        }
        logger.info("Request to web server successful");
    }

    public void postFilename(String hash, String text) throws UnirestException {
        HttpResponse<String> response1 = new HttpRequestWithBody(HttpMethod.PUT,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "filenames")
                .body(text)
                .asString();
        if (response1.getStatus() == 404) {
            HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.POST,
                    "http://localhost:8080/v1/contents/{hash}/{resource}")
                    .routeParam("hash", hash)
                    .routeParam("resource", "filenames")
                    .body(text)
                    .asString();
            if (response.getStatus() != 201) {
                logger.info("Request to web server failed " + response.getStatusText());
            }
        }
        logger.info("Request to web server successful");
    }


    public List<DescriptionXML> getDescriptions() throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "descriptions")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<DescriptionXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new DescriptionXML((String) hmap.get("hash"), (List<String>) hmap.get("description")));
        }
        return retval;
    }

    public List<DescriptionXML> getDescriptions(String hash) throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "descriptions")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<DescriptionXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new DescriptionXML((String) hmap.get("hash"), (List<String>) hmap.get("description")));
        }
        return retval;
    }

    public void deleteDescriptions() throws UnirestException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.DELETE,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "descriptions")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
        }
        logger.info("Request to web server successful");
    }

    public void postDescription(String hash, String text) throws UnirestException {
        HttpResponse<String> response1 = new HttpRequestWithBody(HttpMethod.PUT,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "descriptions")
                .body(text)
                .asString();
        if (response1.getStatus() == 404) {
            HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.POST,
                    "http://localhost:8080/v1/contents/{hash}/{resource}")
                    .routeParam("hash", hash)
                    .routeParam("resource", "descriptions")
                    .body(text)
                    .asString();
            if (response.getStatus() != 201) {
                logger.info("Request to web server failed " + response.getStatusText());
            }
        }
        logger.info("Request to web server successful");
    }


    public List<TagXML> getTags() throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "tags")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<TagXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new TagXML((String) hmap.get("hash"), (List<String>) hmap.get("tag")));
        }
        return retval;
    }

    public List<TagXML> getTags(String hash) throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "tags")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
            return null;
        }
        logger.info("Request to web server successful");
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        List<TagXML> retval = new LinkedList<>();
        for (Object XML : XMLs) {
            LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XML;
            retval.add(new TagXML((String) hmap.get("hash"), (List<String>) hmap.get("tag")));
        }
        return retval;
    }

    public void deleteTags() throws UnirestException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.DELETE,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "tags")
                .asString();
        if (response.getStatus() != 200) {
            logger.info("Request to web server failed");
        }
        logger.info("Request to web server successful");
    }

    public void postTag(String hash, String text) throws UnirestException {
        HttpResponse<String> response1 = new HttpRequestWithBody(HttpMethod.PUT,
                "http://localhost:8080/v1/contents/{hash}/{resource}")
                .routeParam("hash", hash)
                .routeParam("resource", "tags")
                .body(text)
                .asString();
        if (response1.getStatus() == 404) {
            HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.POST,
                    "http://localhost:8080/v1/contents/{hash}/{resource}")
                    .routeParam("hash", hash)
                    .routeParam("resource", "tags")
                    .body(text)
                    .asString();
            if (response.getStatus() != 201) {
                logger.info("Request to web server failed " + response.getStatusText());
            }
        }
        logger.info("Request to web server successful");
    }
}
