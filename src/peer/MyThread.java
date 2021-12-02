package peer;

public abstract class MyThread extends Thread {
    public abstract boolean isFinished();
    public abstract void write_file();
}
