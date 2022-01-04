package peer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("unchecked")
public class ServiceClient {
    public static void main(String[] args) throws UnirestException, JsonProcessingException {
        getTags();
    }

    // TODO Encapsulate totes les connexions al server
    public static List<String> getTags() throws UnirestException, JsonProcessingException {
        HttpResponse<String> response = new HttpRequestWithBody(HttpMethod.GET,
                "http://localhost:8080/v1/resources/{resource}")
                .routeParam("resource", "tags")
                .asString();
        System.out.println(response.getStatusText());
        List<?> XMLs = new XmlMapper().readValue(response.getBody(), List.class);
        LinkedHashMap<String, Object> hmap = (LinkedHashMap<String, Object>) XMLs.get(0);
        System.out.println(hmap.get("hash"));
        System.out.println(hmap.get("tag"));
        LinkedHashMap<String, Object> hmap2 = (LinkedHashMap<String, Object>) XMLs.get(1);
        System.out.println(hmap2.get("hash"));
        System.out.println(hmap2.get("tag"));
        return null;
    }
}
