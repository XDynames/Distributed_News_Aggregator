import java.util.concurrent.Semaphore;
import javax.xml.bind.JAXBException;
import java.io.ObjectOutputStream;
import java.util.PriorityQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Runnable;
import java.util.HashMap;
import java.net.Socket;
// Super class for worker threads
public class Worker implements Runnable{
	// Testing Flag and Event Logger
	public static boolean TEST = true;
	protected Logger mLogger;

	protected Job mJob;
	protected FeedLock mFeedLock;
	protected LamportClock mClock;
	protected PriorityQueue<Job> mQueue;
	// Shared memory resource locks
	protected MutableBoolean mAggregateRequired;
	protected TimesMapLock mTimesMapLock;
	protected Semaphore mFileLock;
	// File system access lock
	protected ContentMapLock mContentMapLock;

	public Worker(Job job, AggregationServer server){
		mJob = job;
		mQueue = server.getQueue();
		mClock = server.getClock();
		mLogger = server.getLogger();
		mFileLock = server.getFileLock();
		mFeedLock = server.getFeedLock();
		mTimesMapLock = server.getTimesMapLock();
		mContentMapLock = server.getContentMapLock();
		mAggregateRequired = server.getAggregateRequired();
	}
	
	public void run(){}

	// Aggregates the records in the hash-map into an ATOM feed
	protected void aggregate() throws InterruptedException, IOException{
        PriorityQueue<ClientRecord> orderedRecords;
        ArrayList<Content> orderedEntries = new ArrayList<Content>();
        orderedRecords = new PriorityQueue<ClientRecord>(11,
                                         new RecordComparator());

        // Read both of the client's content maps
        HashMap<String,Content> contentMap = mContentMapLock.read();
        HashMap<String,Times> timesMap = mTimesMapLock.read();
        // Construct client records from the two maps
        for (String key: timesMap.keySet()){
            if (!contentMap.containsKey(key)){ break; }
            Content content = contentMap.get(key);
            Times times = timesMap.get(key);
            ClientRecord record = new ClientRecord(content, times);
            orderedRecords.add(record);
        }

        while(orderedRecords.size() > 0)
            { orderedEntries.add(orderedRecords.poll().getContent()); }

        mFeedLock.write(orderedEntries, "", false);

        if (TEST && mLogger != null){
        	String details = feedToString();
        	writeToLog("Entry Aggregation", details);
        }
    }



	//		==== XML TRANSLATION METHODS ===
	protected String contentToXMLString(Content content) 
									throws IOException, JAXBException
		{ return Translator.objToString(content); }

	// 		==== NETWORK COMMUNICATION METHODS ===
	// Writes the provided HTTP response to the socket
	protected void sendReply(String reply) 
							throws IOException, InterruptedException {
		ObjectOutputStream outputStream;
		if (mJob == null){ reply += "Socket Closed, failed to reply"; }
		else { Socket socket = mJob.getSocket();
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			outputStream.writeObject(reply);
			// Disconnect from server
			outputStream.close();
			socket.close(); 
		}
		// Writes the reply to the event log
		if (TEST && mLogger != null){ writeToLog(reply); }
	}

	// Return a HTTP compliant message indicating an error
	// occurred while processing the request as a String
	protected String buildReplyUnSucessful(){
		return HTTPHelper.createHttpResponse("AggregationServer",
										500, mClock.getTime());
	}

	// Returns a HTTP compliant message indicating the
	// request was malformed or invalid
	protected String buildReplyFailed() {
		return HTTPHelper.createHttpResponse("AggregationServer",
											400, mClock.getTime()); 
	}

	// Returns a HTTP compliant message indicating the
	// request had been successfully filled as a String
	protected String buildReplySucessful(String type, String uuid, int status){
		String reply;
		switch (type){
			case "Put": reply = HTTPHelper.createHttpResponse(type, 
								 mJob.getContent(),	uuid, "AggregationServer",
								 status, mClock.getTime());
								 break;
			case "Get":
			case "Head": reply = HTTPHelper.createHttpResponse(type,
								 mJob.getContent(), mJob.getUUID(),
								"AggregationServer", status,	mClock.getTime());
								 break;
			default: reply = buildReplyUnSucessful();
		}
		return reply;
	}

	// Returns the aggregated feeds as a formated xml string
    private String feedToString(){
        String feed = "";
        try{ feed = Translator.objToString(mFeedLock.read()); }
        catch (Exception e ){ e.printStackTrace(); }
        return feed;
    }

    // Write an event to the log file
    protected void writeToLog(String event, String details)
    						throws IOException, InterruptedException{
        mFileLock.acquire();
        // Critical Section
        mLogger.logEvent(event,details);
        // Critical Section
        mFileLock.release();
    }

    // Overload that writes the sent message to the log file
	protected void writeToLog(String message) 
							throws IOException, InterruptedException{
		mFileLock.acquire();
		// Critical Section
		mLogger.logMessage(true, message);
		// Critical Section
		mFileLock.release();
	}
}