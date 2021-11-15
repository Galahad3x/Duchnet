package peer;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Peer extends Remote {
    PeerInfo share_address() throws RemoteException;
    void add_node(PeerInfo info) throws RemoteException;
    List<Content> list_files_all_network(List<PeerInfo> visited_peers) throws RemoteException;
    List<PeerInfo> find_seed(Content file, List<PeerInfo> visited_peers) throws RemoteException;
}
