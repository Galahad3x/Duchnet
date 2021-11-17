package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Manager extends Remote {
    /**
     * Given a hash, return the Content with a file_data, called remotely
     * @param hash the hash of the file to return
     * @return a Content with its file_data
     * @throws Exception if something fails
     */
    Content download_file(String hash) throws Exception;
}
