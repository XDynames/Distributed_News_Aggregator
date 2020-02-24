import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;

// Handles reader-writer problem that arises when
// managing shared resources. 
// Orders queued threads based on Lamport clock of
// their Job.
// Allows for simultaneous read operations to occur
// until a write is required which locks the resource
// to all other operations until it is finished
abstract class  ReadWriteLock <T,C>{
	private Semaphore queueLock;
	private Semaphore readerCountAccess;
	private Semaphore resourceAccess;
	private int readerCount;

	public ReadWriteLock(){
		resourceAccess = new Semaphore(1, true);
		readerCountAccess = new Semaphore(1);
		queueLock = new Semaphore(1, true);
		readerCount = 0;
	}

	// Thread safe method to read the shared resource
	public T read() throws InterruptedException {
		T read = null;

		queueLock.acquire();
		// Increment reader count
		readerCountAccess.acquire();
		readerCount++;
		// First reader takes control of resource
		if (readerCount == 1){ resourceAccess.acquire(); }
		// Dequeues next thread in line 
		queueLock.release();
		readerCountAccess.release();

		// Critical Section
		read = doRead();
		// Critical Section

		// Decrement reader count
		readerCountAccess.acquire();
		readerCount--;
		// Last reader releases the resource
		if (readerCount == 0){ resourceAccess.release(); }
		readerCountAccess.release();

		return read;
	}

	// Thread safe method to write to the shared resource
	public void write(C newData, String key, Boolean delete)
							throws InterruptedException {
		queueLock.acquire();
		// No additional threads dequeue until writer
		// has control of the resource
		resourceAccess.acquire();
		queueLock.release();
		
		// Critical Section
		doWrite(newData, key, delete);
		// Critical Section

		resourceAccess.release();
	}

	// Implemented in derived class
	abstract T doRead();
	abstract void doWrite(C newData, String key,
								Boolean delete);
}

// Implementation of Reader-Writer lock for client records
class TimesMapLock extends ReadWriteLock <HashMap<String,Times>,Times>{
	private HashMap<String,Times> mTimesMap;
	public TimesMapLock(){
		super();
		mTimesMap = new HashMap<String,Times>();
	}
	// Class specific access or modify methods
	@Override
	public HashMap<String,Times> doRead(){
		HashMap<String,Times> copy;
		copy = new HashMap<String,Times>();
		// Creates a copy of the hash-map 
		for (String key : mTimesMap.keySet()){
			Times time = new Times();
			time.copy(mTimesMap.get(key));
			copy.put(key, time);
		}
		return copy; 
	}
	@Override
	public void doWrite(Times newData,
						 String key, Boolean delete) {
		if (delete) { mTimesMap.remove(key); } 
		else  { mTimesMap.put(key, newData); }
	}
}

// Implementation of Reader-Writer lock for client content
class ContentMapLock extends ReadWriteLock <HashMap<String,Content>,Content>{
	private HashMap<String,Content> mContentMap;
	public ContentMapLock(){
		super();
		mContentMap = new HashMap<String,Content>();
	}
	@Override
	public void doWrite(Content newData,
						 String key, Boolean delete) {
		if (delete) { mContentMap.remove(key); }
		else  { mContentMap.put(key, newData); }
	}
	@Override
	public HashMap<String,Content> doRead() {
		HashMap<String,Content> copy;
		copy = new HashMap<String,Content>();
		// Creates a copy of the hash-map
		for (String key: mContentMap.keySet()){
			Content content = new Content();
			content.copy(mContentMap.get(key));
			copy.put(key, content);
		}
		return copy;
	}
}

// Implementation of Reader-Writer lock for the aggregated ATOMFeed
class FeedLock extends ReadWriteLock <ATOMFeed, ArrayList<Content>>{
	private ATOMFeed mFeed;
	public FeedLock(String filename){
		super();
		mFeed = initFeed(filename);
	}
	@Override
	public void doWrite(ArrayList<Content> newData,
							 		String key, Boolean delete)
		{ mFeed.setEntries(newData); }
	@Override
	public ATOMFeed doRead(){
		ATOMFeed copy = new ATOMFeed();
		copy.copy(mFeed);
		return copy;
	}

	// Adds informational fields to the servers feed
    private ATOMFeed initFeed(String filename){
        ATOMFeed feed;
        try{ feed = Translator.feedFileToObject(filename); }
        catch (Exception e) { e.printStackTrace(); feed = null; }
        return feed;
    }
}
