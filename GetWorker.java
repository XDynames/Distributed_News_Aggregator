import java.io.ObjectOutputStream;
import java.lang.Runnable;
import java.net.Socket;

public class GetWorker extends Worker implements Runnable{

	public GetWorker(Job job, AggregationServer server)
		{ super(job, server); }

	public void run(){
		String reply, xml;
		// Increment Logical Clock
		mClock.incrementTime();
		try {
			// Update the feed object if required
			if (mAggregateRequired.getBool()){ aggregate(); }
			// Convert the feed object into an XML string
			xml = contentToXMLString(mFeedLock.read());
			mJob.setContent(xml);
			// Respond 200 with content attached
			reply = buildReplySucessful("Get","", 200);
		// If the request failed respond 500
		} catch (Exception e) { e.printStackTrace(); reply = buildReplyUnSucessful(); }
		// Send the response
		try { sendReply(reply); }
		catch (Exception e) { e.printStackTrace(); }	
	}
}