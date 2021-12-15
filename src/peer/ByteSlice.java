package peer;

import java.io.Serializable;

/**
 * A chunk of maximum 1 MB of data from a file
 */
public class ByteSlice implements Serializable {
    private final byte[] bytes;
    private final int bytes_written;

    public ByteSlice(byte[] bytes, int bytes_written) {
        this.bytes = bytes;
        this.bytes_written = bytes_written;
    }

    public int getBytes_written() {
        return bytes_written;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
