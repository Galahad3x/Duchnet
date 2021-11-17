package peer;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Peer extends Remote {
    /**
     * Add the Peer and Manager of another node, not remote
     * @param info The PeerInfo of this other node
     * @throws RemoteException When remote calls fail
     */
    void add_node(PeerInfo info) throws RemoteException;

    /**
     * Lists all the files available in the network, called back recursively
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @return List with all the contents with file_data = null;
     * @throws RemoteException if remote calls fail
     */
    List<Content> list_files_all_network(List<PeerInfo> visited_peers) throws RemoteException;

    /**
     * Find nodes that own a certain file
     * @param file the file we are looking for
     * @param visited_peers List of all the PeerInfo's visited by this request
     * @return List of all the PeerInfo's that own the file
     * @throws RemoteException if remote calls fail
     */
    List<PeerInfo> find_seed(Content file, List<PeerInfo> visited_peers) throws RemoteException;
}