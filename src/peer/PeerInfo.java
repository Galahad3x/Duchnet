package peer;

import java.io.Serializable;

/**
 * Encapsulation of an IP and port
 */
public class PeerInfo implements Serializable {
    /**
     * IP of a node
     */
    public String ip;
    /**
     * Port of the node
     */
    public Integer port;

    /**
     * Constructor for PeerInfo
     * @param ip IP of the node
     * @param port Port of the node
     */
    public PeerInfo(String ip, Integer port){
        this.ip = ip;
        this.port = port;
    }

    /**
     * Returns a String of the PeerInfo
     * @return IP:PORT
     */
    public String toString(){
        return ip + ":" + port.toString();
    }
}
