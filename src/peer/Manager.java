package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;

public interface Manager extends Remote {
    /**
     * Given a hash, return the Content with a file_data, called remotely
     * @param hash the hash of the file to return
     * @return a Content with its file_data
     * @throws Exception if something fails
     */
    byte[] get_slice(String hash, Integer slice) throws Exception;
    Integer getSlicesNeeded(String hash) throws Exception;
}
