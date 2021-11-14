package peer;

import java.io.Serializable;

public class PeerInfo implements Serializable {
    public String ip;
    public Integer port;
    public PeerInfo(String ip, Integer port){
        this.ip = ip;
        this.port = port;
    }

    public String toString(){
        return ip + ":" + port.toString();
    }
}
