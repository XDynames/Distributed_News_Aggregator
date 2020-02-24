import java.io.ObjectOutputStream;
import java.util.PriorityQueue;
import java.lang.Runnable;
import java.util.HashMap;
import java.net.Socket;
import java.util.Date;

public class HeadWorker extends Worker implements Runnable{

	public HeadWorker(Job job, AggregationServer server)
		{ super(job, server); }

	public void run(){
		String reply;
		// Increment Logical Clock
		mClock.incrementTime();
		// Attempt to fill the Head request
		switch (mJob.getAgentName()){
			case "Heartbeat": reply = servicePutHeadRequest(); break;
			case "GetClient": reply = serviceGetHeadRequest(); break;
			default: reply = buildReplyUnSucessful(); 
		}
		// Write the reply to the client
		try{ sendReply(reply); } catch (Exception e){ e.printStackTrace(); }	
	}

	private void queueCheckpointJob(Date timeStamp){
		mQueue.add(new Job(mClock.getTime(), "Checkpoint", null, 
											mJob.getUUID(), new Date()));
	}


	// Preforms update of last client contact on the relevant record and returns
	// a HTTP message indicating the success or failure of the actions
	private String servicePutHeadRequest(){
		String xml, reply;
		Content content;
		// Increment logical time
		mClock.incrementTime();
		// Attempt to fill put client's head request
		try {
			Date timeStamp = new Date();
			Times times = new Times(timeStamp, mClock.getTime());
			// Queue a job to update the client's backup
			queueCheckpointJob(timeStamp);
			// Update clients last contact time stamp
			mTimesMapLock.write(times, mJob.getUUID(), false);
			// Retrieve client's content and marshal to XML
			content = mContentMapLock.read().get(mJob.getUUID());
			if (content == null){ mJob.setContent(""); }
			else { mJob.setContent(contentToXMLString(content)); }
			// Generate Head request reply
			reply = buildReplySucessful("Head","", 200);
		// If request Failed respond with 500
		} catch (Exception e) {e.printStackTrace(); reply = buildReplyUnSucessful(); }
		return reply;
	}

	// Returns the meta-data of the current aggregated feed to the get client
	private String serviceGetHeadRequest(){
		String xml, reply;
		// Attempt to fill put client's head request
		reply = buildReplyUnSucessful();
		return reply;
	}
}