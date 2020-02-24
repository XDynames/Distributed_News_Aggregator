import java.net.UnknownHostException;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.lang.Runnable;
import java.nio.file.Path;
import java.io.File;

public class PutClient extends Client implements Runnable{
	
	private MutableBoolean mHeartbeatStatus;
	private Heartbeat mHeartbeat;
	private String mFilename;
	private int mLocalHash;
	private int mETag;

	public PutClient(int port, int clock, String uuid, String filename){ 
		super(port, clock, uuid);
		mFilename = filename;
		mHeartbeatStatus = new MutableBoolean(true);
	}
	public PutClient(int port, int clock, String filename){ 
		super(port, clock);
		mFilename = filename;
		mHeartbeatStatus = new MutableBoolean(true);
	}
	public PutClient(int port, String filename){
		super(port);
		mFilename = filename;
		mHeartbeatStatus = new MutableBoolean(true);
	}
	public PutClient(int port, String filename, Logger logger){
		super(port, logger);
		mFilename = filename;
		mHeartbeatStatus = new MutableBoolean(true);
	}

	// Ensures that the clients contents are serialised and recorded
	// Returns an XML string
	private String marshalContent(String contentFilename) 
									throws IOException, JAXBException
	{ return Translator.fileToXMLString(contentFilename); }

	// Requests update of content held by the aggregation server
	private HTTPResponse requestContentUpdate(){
		HTTPResponse response = null;
		String xmlContent;
		try{
			if (mFilename == ""){ xmlContent = "null"; }
			else { xmlContent = marshalContent(mFilename+".txt"); }
			mLocalHash = xmlContent.hashCode();
			response = requestService("Put", xmlContent);
		} catch (Exception e) { e.printStackTrace(); }
		return response;
	}

	// Records the UUID and hash received from the server to file
	private void saveCookie(int hash){
		String dir = "./ContentServer/";
		Path idPath = Paths.get(dir + mFilename + ".meta");
		String metaInfo = "UUID," + mUUID + "\r\n";
		metaInfo += "ETag," + String.valueOf(hash);
		try { // Creates a parent folder if required
			if (!Files.exists(Paths.get(dir)))
				{ Files.createDirectories(Paths.get(dir)); }
			// Record client's UUID and last received hash
			Files.write(idPath, metaInfo.getBytes());
		} catch (Exception e) { e.printStackTrace(); }

		// Logs the updated meta information now stored locally
		if (TEST && mLogger != null){
			String details = "Cookie and Etag saved locally \r\n";
			details += "UUID: " + mUUID + "\r\n";
			details += "ETag: " + mETag + "\r\n";
			try{ mLogger.logEvent("Meta Info Update", details); }
			catch(Exception e){ e.printStackTrace(); }
		}
	}

	// Returns the content of a file object as a string
	private String readFile(File file) 
						throws IOException, JAXBException
		{ return Translator.readFile(file.toString()); }

	// Load information from the clients meta file
	private void loadMetaInformation(String metaInfo){
		for (String line : metaInfo.split("\r\n")){
			String[] words = line.split(",");
			switch (words[0]){
				case "UUID": mUUID = words[1]; break;
				case "ETag": mETag = Integer.valueOf(words[1]); break;
				default: break;
			}
		}

		// Logs the client intialising from file
		if (TEST && mLogger != null){
			String details = "Loaded Meta Information \r\n";
			details += "UUID: " + mUUID + "\r\n";
			details += "ETag: " + mETag + "\r\n";
			try{ mLogger.logEvent("Checkpoint Load", details); }
			catch (Exception e){ e.printStackTrace(); }
		}
	}

	// Restores client's state on restart
	private void initialise() 
						throws IOException, JAXBException{
		String dir = "./ContentServer/";
		Path idPath = Paths.get(dir + mFilename + ".meta");
		try{
			// Load details from meta file if available
			if (!Files.exists(idPath)) { return; }
			File file = new File(idPath.toString());
			loadMetaInformation(readFile(file)); 
		} catch (Exception e) { e.printStackTrace(); }
	}

	public void run(){
		// Look for previous client information and load it
		try { initialise(); }
		catch (Exception e){ e.printStackTrace(); }
		
		if (TEST) { probe("2"); }
		// Requests to have feed hosted on server
		HTTPResponse response = requestContentUpdate(); 
		// Keep the cookie the server has assigned the client
		mUUID = response.getUUID();
		// Check that the content hosted is consistent with
		// what was sent to the server
		int eTag = response.getHash();
		// Write cookie and ETag to meta-file
		saveCookie(eTag);
		// Start heartbeat thread
		mHeartbeat = new Heartbeat(this);
		Thread heart = new Thread(mHeartbeat);
		heart.start();
		// Resend content if heartbeat fails to get same ETag or
		// if the request has failed or timed out
	}

	// Accessors
	public int getHash(){ return mLocalHash; }
	public MutableBoolean getHeartbeatStatus(){ return mHeartbeatStatus; }

	public void stop(){ super.stop(); mHeartbeat.stop(); }

	public static void main(String[] args){
		PutClient putClient = new PutClient(1777, args[0]);
		putClient.run();
	}
}

// Heartbeat thread
class Heartbeat extends Client implements Runnable{
	private int mLocalHash;
	private MutableBoolean mHeartbeatStatus;

	public Heartbeat(PutClient putClient){
		super(putClient.getPort(), putClient.getUUID());
		mHeartbeatStatus = putClient.getHeartbeatStatus();
		mLocalHash = putClient.getHash();
		mLogger = putClient.getLogger();
		mClock = putClient.getClock();
		mIsStopped = false;
	}

	public void run(){
		HTTPResponse response;
		// Send heartbeats to the server at 3 second intervals
		do { try { Thread.sleep(3000); } 
			 catch (Exception e ){ e.printStackTrace(); }
			 response = heartbeat();
			 if (!isStopped() || response == null) { break; } }
		// Provided content hashes are equal and responses are
		// positively acknowledged continue
		while (response.getStatus() == 200 &&
				 			response.getHash() == mLocalHash);
		// Indicates that the heartbeat tests have failed
		mHeartbeatStatus.setBool(false);
	}

}