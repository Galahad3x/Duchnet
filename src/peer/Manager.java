package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Manager extends Remote {
    byte[] download_file(String hash) throws RemoteException;
}
