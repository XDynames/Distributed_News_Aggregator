import java.util.concurrent.Semaphore;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.PriorityQueue;
import java.io.IOException;
import java.lang.Runnable;
import java.net.Socket;

public class Queuer implements Runnable{
	// Testing Flag and Event Logger
	public static boolean TEST = true;
	private Logger mLogger;
	
	private Socket mSocket;
	private PriorityQueue<Job> mQueue;
	private LamportClock mLamportClock;
	private Semaphore mFileLock;

	public Queuer(Socket socket, Listener listener){
		mLamportClock = listener.getClock();
		mFileLock = listener.getFileLock();
		mLogger = listener.getLogger();
		mQueue = listener.getQueue();
		mSocket = socket;
	}

	// Wrapper for constructing jobs from HTTPRequests
	private Job createJob(HTTPRequest request){
		return new Job(request.getTime(), request.getType(), mSocket,
							request.getContent(), request.getUUID(),
							  				request.getAgentName());
	}

	public void run(){
		ObjectInputStream inputStream;
		try{
			// Receive Incoming HTTP Request
			inputStream = new ObjectInputStream(mSocket.getInputStream());
			String httpRequest = (String) inputStream.readObject();
			// Add the received message to the test log
			if (TEST && mLogger != null){ writeToLog(false, httpRequest); }
			// Testing for timeouts
			if (httpRequest.equals("2")){ Thread.sleep(1000); }
			// Priority responses to ping requests
			if (httpRequest.getBytes().length == 1){ sendPing(); return; }
			// Parse Request
			HTTPRequest request = HTTPHelper.parseHttpRequest(httpRequest);
			// Update Logical Clock
			mLamportClock.incrementTime(request.getTime());
			// Create job item and place in queue
			Job job = createJob(request);
			mQueue.add(job);
		} catch (Exception e) { e.printStackTrace(); }
	}

	// Sends a ping back to the client
	private void sendPing() throws IOException {
		ObjectOutputStream outputStream;
		outputStream = new ObjectOutputStream(mSocket.getOutputStream());
		outputStream.writeObject("1");
		// Disconnect
		outputStream.close();
		mSocket.close();
		// Add the sent message to the test log
		if (TEST && mLogger != null){ 
			try{ writeToLog(true, "1"); }
			catch(Exception e){ e.printStackTrace(); }
		}
	}

	// Writes the received message to the log file
	private void writeToLog(boolean sent, String message) 
							throws IOException, InterruptedException{
		mFileLock.acquire();
		// Critical Section
		mLogger.logMessage(sent, message);
		// Critical Section
		mFileLock.release();
	}

}