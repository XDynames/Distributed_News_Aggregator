import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.lang.Runnable;
import java.net.Socket;

// Thread worker for replying to invalid job requests
public class InvalidWorker extends Worker implements Runnable{
	public InvalidWorker(Job job, AggregationServer server)
		{ super(job, server); }

	// Replies to the requester that the requests type is invalid
	public void run(){
		// Increment Logical Clock
		mClock.incrementTime();
		// Create the 400 HTTP message and send it via the socket
		String reply = buildReplyFailed();
		// Send the reply to the client
		try{ sendReply(reply); }
		catch (SocketException c) { /* Socket closed*/ }
		catch (Exception e) { e.printStackTrace(); }
	}
}