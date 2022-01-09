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
     *
     * @param ip   IP of the node
     * @param port Port of the node
     */
    public PeerInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Rebuilds a PeerInfo from a string
     *
     * @param text the peerInfo in the toString() format
     * @return a PeerInfo
     */
    public static PeerInfo fromString(String text) {
        return new PeerInfo(text.split(":")[0], Integer.parseInt(text.split(":")[1]));
    }

    /**
     * Returns a String of the PeerInfo
     *
     * @return IP:PORT
     */
    public String toString() {
        return ip + ":" + port.toString();
    }
}
