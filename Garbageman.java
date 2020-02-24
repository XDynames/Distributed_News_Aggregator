import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.io.File;

public class Garbageman extends Worker implements Runnable{
	private boolean mIsStopped;

	public Garbageman(AggregationServer server){
		super(null, server);
		mIsStopped = false;
	}

	// Returns the number of seconds elapsed since timeStamp
	private int secondsPast(Date timeStamp){
		long ms = (timeStamp.getTime() - (new Date()).getTime());
		return (int) Math.abs(ms) / 1000;
	}

	// Deletes the xml content hosted for the
	// client assigned to the input uuid
	private void deleteContent(String uuid){
		String dir = "./Server/Checkpoint/";
		String root = dir + uuid;
		String contentPath = root + ".xml";
		File toDelete = new File(contentPath);
		toDelete.delete();	
	}

	// Deletes the meta content hosted for the
	// client assigned to the input uuid
	private void deleteMetaInfo(String uuid){
		String dir = "./Server/Checkpoint/";
		String root = dir + uuid;
		String contentPath = root + ".meta";
		File toDelete = new File(contentPath);
		toDelete.delete();	
	}

	// Removes information backed up on disk
	// for the client with input uuid
	private void deleteBackup(String uuid){
		try{
			mFileLock.acquire();
			// Critical Section
			deleteContent(uuid);
			deleteMetaInfo(uuid);
			// Critical Section
			mFileLock.release();
		} catch (Exception e){ e.printStackTrace(); }
	}

	// Returns true if no contact from the client
	// has been received for 15 seconds
	private Boolean isExpired(Times times)
		{ return secondsPast(times.getTimeStamp()) > 15; }

	// Deletes client information held in memory
	private void removeFromMemory(String uuid){
		// Request write locks and delete
		try{ mTimesMapLock.write(null, uuid, true);
			 mContentMapLock.write(null, uuid, true);
		} catch (Exception e){ e.printStackTrace(); }	
	}

	// Completes garbage collection operation on hosted client content
	public void run(){
		HashMap<String,Times> timesMap = null;
		ArrayList<String> expiredUUIDs;
		while(!isStopped()){
			String removed = "";
			expiredUUIDs = new ArrayList<String>();
			// Request read lock and retrieve copy
			try{ timesMap = mTimesMapLock.read(); }
			catch (Exception e) { e.printStackTrace(); }
			// Test all records for expiry
			for (String key : timesMap.keySet()){
				if (isExpired(timesMap.get(key)))
					{ expiredUUIDs.add(key); }
			}
			// Delete expired information in memory
			for (String uuid : expiredUUIDs){
				removeFromMemory(uuid);
				deleteBackup(uuid);
				removed += uuid + "\r\n";
			}
			// If modification to the records occurred
			// a feed aggregation is required
			if (expiredUUIDs.size() > 0){
				mAggregateRequired.setBool(true);
				if (TEST && mLogger != null){
					// Log which entries have been removed
					try{ writeToLog("Content Removal", removed); }
					catch(Exception e){ e.printStackTrace(); }
				}
			}
			// Check feeds every second
			try{ Thread.sleep(1000); } 
			catch(Exception e){ e.printStackTrace(); }
		}
	}

	private synchronized boolean isStopped(){ return mIsStopped; }
	public void stop(){ mIsStopped = true; }

}