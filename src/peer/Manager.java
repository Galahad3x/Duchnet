package peer;

import java.rmi.Remote;
import java.util.List;

/**
 * Defines the remote functions available for a Manager, which controls contents in a node
 */
public interface Manager extends Remote {
    /**
     * Given a hash, return the Content with a file_data, called remotely
     * @param hash the hash of the file to return
     * @return a Content with its file_data
     * @throws Exception if something fails
     */
    ByteSlice get_slice(String hash, Integer slice) throws Exception;

    /**
     * Return the number of 1 MB slices needed to download a file
     * @param hash The hash of the file we want
     * @return The needed number of slices
     * @throws Exception If something fails
     */
    Integer getSlicesNeeded(String hash) throws Exception;

    /**
     * Used to ask the manager for hashes, used when the file is too big
     * @param hash The hash of the whole file
     * @return List of all the hashes needed
     * @throws Exception If something fails
     */
    List<String> getHashesNeeded(String hash) throws Exception;

    /**
     * Return the descriptions and tags of a content
     * @param hash The hash of the file we want
     * @return The content with descriptions and tags
     * @throws Exception if something fails
     */
    Content get_information(String hash) throws Exception;

    /**
     * Get the filename a file is saved with in the seeder
     * @param hash The hash of the file
     * @param names The names this file might have
     * @return The filename
     * @throws Exception If something fails
     */
    String get_filename(String hash, List<String> names) throws Exception;
}
