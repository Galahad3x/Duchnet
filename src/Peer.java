import java.rmi.RemoteException;
import java.util.List;

public interface Peer {
    public PeerInfo share_address() throws RemoteException;
    public List<Content> list_files_all_network(List<PeerInfo> visited_peers) throws RemoteException;
}
