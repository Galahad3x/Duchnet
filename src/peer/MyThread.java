package peer;

public abstract class MyThread extends Thread {
    public abstract boolean isFinished();
    public abstract void write_file();
    public abstract String get_progress();
    public abstract void create_slice_array() throws Exception;
}
