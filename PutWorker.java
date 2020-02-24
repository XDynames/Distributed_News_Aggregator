import javax.xml.bind.JAXBException;
import java.util.PriorityQueue;
import java.io.IOException;
import java.lang.Runnable;
import java.util.HashMap;
import java.util.Date;
import java.util.UUID;

public class PutWorker extends Worker implements Runnable{

	public PutWorker(Job job, AggregationServer server)
		{ super(job, server); }

	// Provides clients a cookie if they do not have one
	private String getCookie(){
		return (mJob.getUUID().equals("null")) ? 
				UUID.randomUUID().toString() : mJob.getUUID();
	}

	// Unpack clients XML string as a content object
	private Content xmlStringToObject() throws JAXBException{
		String xml = mJob.getContent();
		return Translator.unmarshal(xml);
	}

	// Adds a new job to the queue that will update the client's
	// backup record
	private void queueCheckpointJob(String uuid, String content){
		mQueue.add(new Job(mClock.getTime(), "Checkpoint", content, 
												uuid, new Date()));
	}

	// Returns true is the request cookie has a record on the server
	private Boolean cookieInMap() throws InterruptedException
		{ return mTimesMapLock.read().containsKey(mJob.getUUID()); }
	// Returns true is the requests cookie is uninitialised
	private Boolean cookieIsNull()
		{ return mJob.getUUID().equals("null"); }
	// Returns true is the request content is uninitialised
	private Boolean contentIsNull()
		{ return mJob.getContent().equals("null"); }
	// Returns true if the xml string provided in the request and the serialised
	// local content object held for the client hash to the same code
	private Boolean localContentIsMatch() 
				throws InterruptedException, IOException, JAXBException {
		Content localContent = mContentMapLock.read().get(mJob.getUUID());
		int localHash = contentToXMLString(localContent).hashCode();
		int requestHash = mJob.getContent().hashCode();
		return localHash == requestHash;
	}
	// Adds a new clients information to the server using a
	// new cookie and returns the HTTP response to reply with
	private String createNewClientEntry() 
						throws JAXBException, InterruptedException{
		// Get the client a cookie
		String uuid = getCookie();
		// Add client to time and content maps
		mContentMapLock.write(xmlStringToObject(), uuid, false);
		Times times = new Times(new Date(), mClock.getTime());
		mTimesMapLock.write(times, uuid, false);
		// Create a job to update client's backup
		queueCheckpointJob(uuid, mJob.getContent());
		// Set Aggregation Required flag
		mAggregateRequired.setBool(true);
		// 201 HTTP_CREATED reply with added contents hash
		return buildReplySucessful("Put", uuid, 201);
	}

	// Updates a client information if they have an existing cookie
	// New content overwrites existing, same content is ignored
	// In all cases the client's last seen time is updated
	// Returns the appropriate HTTP response as a String
	private String updateExistingEntry() 
				throws IOException, InterruptedException, JAXBException{
		String contentToBackup = null;
		String uuid = mJob.getUUID();
		if (!localContentIsMatch()){
			// Update the content on the server
			mContentMapLock.write(xmlStringToObject(), uuid, false);
			// Set Aggregation Required flag
			mAggregateRequired.setBool(true);
			// Set content to be added to client's backup
			contentToBackup = mJob.getContent();
		}
		Times times = new Times(new Date(), mClock.getTime());
		// Updates the time stamp of the last client contact
		mTimesMapLock.write(times, uuid, false);
		// Queue a Checkpoint job
		queueCheckpointJob(uuid, contentToBackup);
		// 200 OK response for updating existing content
		return buildReplySucessful("Put", uuid, 200);
	}

	public void run(){ 
		String reply;
		// Increment Logical Clock
		mClock.incrementTime();
		// Attempt to fill the Put request
		try{// No content parsed, reply 204 NO_CONTENT 
			if (contentIsNull())
				{ reply = buildReplySucessful("Put", "", 204); }
			// Client cookie cannot be resolved
			else if (cookieIsNull() || !cookieInMap())
				 { reply = createNewClientEntry(); }
			else { reply = updateExistingEntry(); }
		// Failed respond with 500 Internal Server Error
		} catch (Exception e) 
			{ reply = buildReplyUnSucessful(); } 
		// Send the HTTP response to the client
		try { sendReply(reply); }
		catch (Exception e) { e.printStackTrace(); }
	}
}