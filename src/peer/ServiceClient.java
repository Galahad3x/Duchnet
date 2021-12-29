package peer;

import java.net.URL;
import java.util.List;

public class ServiceClient {
    // TODO Encapsular totes la connexions al servidor
    public List<String> getDescriptions(String hash){
        URL url = new URL("api/v1/resources/descriptions");
        // etc etc
    }
}
