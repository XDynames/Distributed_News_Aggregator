import java.util.concurrent.Semaphore;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.lang.Runnable;
import java.util.PriorityQueue;

public class Listener implements Runnable{
	// Testing Event Logger
	private Logger mLogger;

	private ServerSocket mSocket;
	private boolean mIsStopped;
	private LamportClock mLamportClock;
	private PriorityQueue<Job> mQueue;
	private Semaphore mFileLock;

	public Listener(AggregationServer server){
		mLamportClock = server.getClock();
		mFileLock = server.getFileLock();
		mSocket = server.getSocket();
		mLogger = server.getLogger();
		mQueue = server.getQueue();
	}

	public void run(){
		try{
			Thread transaction;
			while(!isStopped()){
				// Wait for connection on listening socket
				Socket socket = mSocket.accept();
				// Create a new socket thread from the client to communicate on
				transaction = new Thread(new Queuer(socket, this));
				// Spin off the new socket
				transaction.start();
			}
		} catch (SocketException c ){
			// Socket has been forcefully closed
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isStopped(){ return mIsStopped; }
	public void stop(){	mIsStopped = true; }

	// Accessors
	public LamportClock getClock(){ return mLamportClock; }
	public PriorityQueue<Job> getQueue(){ return mQueue; }
	public Semaphore getFileLock(){ return mFileLock; }
	public Logger getLogger(){ return mLogger; }

}