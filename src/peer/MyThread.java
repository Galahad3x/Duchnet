package peer;

public abstract class MyThread extends Thread {
    /**
     * Check if a thread has finished
     * @return True if the thread can be joined
     */
    public abstract boolean isFinished();

    /**
     * Write byte data into a file
     */
    public abstract void write_file();

    /**
     * Get the progress of this thread
     * @return hash if DownloadThread, hash:threads_finished/total_threads if FileThread
     */
    public abstract String get_progress();

    /**
     * Allocate enough space to download a file
     * @throws Exception If the remote call fails
     */
    public abstract void create_slice_array() throws Exception;
}
