import java.util.concurrent.Semaphore;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.PriorityQueue;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.lang.Runnable;
import java.util.HashMap;
import javax.xml.bind.*;
import java.util.Date;
import java.io.File;

public class AggregationServer implements Runnable{
    // Testing flags and logger
    public static boolean TEST = true;
    private Logger mLogger;

    private MutableBoolean mAggregateRequired;
    private PriorityQueue<Job> mQueue;
    private LamportClock mClock;
    private boolean mIsStopped;
    private ServerSocket mServerSocket;
    // Reader-writer locks containing shared resources
    private ContentMapLock mContentMapLock;
    private TimesMapLock mTimesMapLock;
    private FeedLock mFeedLock;
    // Single thread lock for file system access
    private Semaphore mFileLock;
    // References to persistent thread objects
    private Garbageman mGarbageman;
    private Listener mListener;

    public AggregationServer(int portnumber, String filename) 
                                                throws IOException{
        mQueue = new PriorityQueue<Job>(11, new JobComparator()); 
        mServerSocket = new ServerSocket(portnumber);
        mClock = new LamportClock();
        mAggregateRequired = new MutableBoolean();
        mIsStopped = false;
        // Instance locked resources
        mContentMapLock = new ContentMapLock();
        mFeedLock = new FeedLock(filename);
        mTimesMapLock = new TimesMapLock();
        mFileLock = new Semaphore(1, true);
    }

    // Initialisation for default port 4567
    public AggregationServer(String filename) throws IOException
        { this(4567, filename); }

    // Allows for an event logger to be included
    public AggregationServer(int portnumber, String filename,
                              Logger logger) throws IOException{
        this(portnumber, filename);
        mLogger = logger;
    }
    public AggregationServer(String filename, Logger logger)
                                          throws IOException {
        this(4567, filename);
        mLogger = logger;
    }


    // Main thread loop
    public void run() {
        // Load previous state
        intitServer(); 
        // Starts listener and garbageman
        startServices();
        while (!isStopped()){
            // Assign queued jobs to workers
            if (!mQueue.isEmpty())
                { startWorker(mQueue.poll()); }
        }
    }

    // Thread safe access to mIsStopped
    private synchronized boolean isStopped()
        { return mIsStopped; }

    // Stop service persistent threads
    public void stop(){
        if (mGarbageman != null)
            { mGarbageman.stop(); }
        if (mListener != null)
            { stopListener(); }
        mIsStopped = true;
    }

    // Stops the listening thread
    private void stopListener(){
        mListener.stop();
        try{ mServerSocket.close(); }
        catch(Exception e){ e.printStackTrace(); }
    }

    // Initiates a thread to service the passed Job
    private void startWorker(Job job){
        Worker worker;
        if (job != null){
            switch (job.getType()){
                case "Put":  worker = new PutWorker(job, this); break;
                case "Get":  worker = new GetWorker(job, this); break;
                case "Head": worker = new HeadWorker(job, this); break;
                case "Checkpoint": worker = new Checkpointer(job, this); break;
                default:  worker = new InvalidWorker(job, this); break;
            }
        } else { worker = new InvalidWorker(job, this); }
        Thread thread = new Thread(worker);
        thread.start();
    }

    // Converts formated date strings into Date objects
    private Date parseDateString(String dateString) throws ParseException{
        SimpleDateFormat dateFormater;
        String datePattern = "EEE MMM dd kk:mm:ss z yyyy";
        dateFormater = new SimpleDateFormat(datePattern);
        return dateFormater.parse(dateString);
    }

    // Reads the provided file object in as a string
    private String readFile(File file) throws IOException, JAXBException
        { return Translator.readFile(file.toString()); }

    // Adds the appropriate type of time to the Times object
    private void addTime(String keyWord, String time, Times times) 
                                                throws ParseException{
        switch(keyWord){
            case "TimeStamp":
                times.setTimeStamp(parseDateString(time)); break;
            case "LogicalTime":
                int logicalTime = Integer.valueOf(time);
                // Loads servers logical time
                mClock.loadTime(logicalTime);
                times.setTime(logicalTime); break;
            default: break;
        }
    }

    // Loads information from Client's meta file into ClientRecords
    private void loadMeta(File file, String uuid){        
        Times times = new Times();
        // Read file in as a string
        try{
            // Parse string and add information to record
            for (String line : readFile(file).split("\r\n")){
                String[] words = line.split(",");
                addTime(words[0], words[1], times);
            }

            mTimesMapLock.write(times, uuid, false);
        } catch(Exception e) { e.printStackTrace(); }
    }

    // Loads information from client's xml file into ClientRecord
    private void loadXML(File file, String uuid){
        Content content = null;
        // Read file and unmarshall to content object
        try{ 
            content = Translator.unmarshal(readFile(file));
            mContentMapLock.write(content, uuid, false);
        } catch (Exception e ){ e.printStackTrace(); }
    }

    // Uses each file in the file list to restore clients hosted
    // information and the servers logical clock
    private void loadFiles(File[] files){
        // If no files are present no further action required
        if (files == null){ return; }
        String uuid, fileType;
        // Load each file in the list of files
        for (File file : files) {
            if(TEST && mLogger != null){
                try{ writeToLog("Loading file: " + file.getName(),"");}
                catch(Exception e){ e.printStackTrace(); }
            }
            // Extract filename and type
            uuid = file.getName().split("\\.")[0];
            fileType = file.getName().split("\\.")[1];
            // Load the file's information
            switch (fileType){
                case "meta": loadMeta(file, uuid); break;
                case "xml":  loadXML(file, uuid); break;
                default: break;
            }
        }
    }

    // Returns a list of the files in the directory
    private File[] getFiles(String folderPath){
        File serverFolder = new File("./Server/Checkpoint/");
        return serverFolder.listFiles();
    }

    // Restores server state with tombstone files found
    private void intitServer(){
        loadFiles(getFiles("./Server/Checkpoint/"));

        // Log the meta data and content loaded into memory
        if (TEST && mLogger != null){
            String details = summariseLoadedRecords();
            try{ writeToLog("Loaded Checkpoints", details);}
            catch(Exception e){ e.printStackTrace(); }
        }

        // Aggregate loaded entries into a feed
        Worker worker = new Worker(null, this);
        try { worker.aggregate(); } 
        catch (Exception e) { e.printStackTrace(); }
    }

    // Initiates parallel services
    private void startServices(){
        startListening();
        startGarbageman();
    }

    // Starts a thread that listens for client requests
    private void startListening() {
        mListener = new Listener(this);
        Thread listeningThread = new Thread(mListener);
        listeningThread.start();
        if (TEST && mLogger != null){
            try{ writeToLog("Listening Thread Started",""); }
            catch(Exception e){ e.printStackTrace(); }
        }
    }

    // Starts a Garbageman thread to maintain expiry of feeds
    private void startGarbageman(){
        mGarbageman = new Garbageman(this);
        Thread garbagemanThread = new Thread(mGarbageman);
        garbagemanThread.start();
        if (TEST && mLogger != null){
            try{ writeToLog("Garbageman Thread Started","");}
            catch(Exception e){ e.printStackTrace(); }
        }
    }


    // Accessors
    public ContentMapLock getContentMapLock(){ return mContentMapLock; }
    public MutableBoolean getAggregateRequired(){ return mAggregateRequired; }
    public TimesMapLock getTimesMapLock(){ return mTimesMapLock; }
    public ServerSocket getSocket(){ return mServerSocket; }
    public PriorityQueue<Job> getQueue(){ return mQueue; }
    public Semaphore getFileLock(){ return mFileLock; }
    public FeedLock getFeedLock(){ return mFeedLock; }
    public LamportClock getClock(){ return mClock; }
    public Logger getLogger(){ return mLogger; }


    /*                  === TESTING CODE ===
            For inspection of runtime behavior of the 
            aggregation server set TEST = true in the
            member variable declaration 
    */

    // Wrapper for requesting a write to the log file
    private void writeToLog(String event, String details) 
                                    throws IOException, InterruptedException{
        mFileLock.acquire();
        // Critical Section
        mLogger.logEvent(event,details);
        // Critical Section
        mFileLock.release();
    }

    // Creates a string summarising the details of objects loaded from 
    // checkpoint files during initialisation
    private String summariseLoadedRecords(){
        String summary = TestUtil.getHeading("LOADED RECORDS");
        try {
            HashMap<String,Content> contentMap = mContentMapLock.read();
            HashMap<String,Times> timesMap = mTimesMapLock.read();
            for (String key : timesMap.keySet()){
                if (!contentMap.containsKey(key)) { break; }
                summary += "\t" + key + "\r\n";
                summary += TestUtil.getDivider();
                summary += "Last contact wall clock time: ";
                summary += timesMap.get(key).getTimeStamp().toString() +"\r\n";
                summary += "Last contact Logical time: ";
                summary += String.valueOf(timesMap.get(key).getTime()) +"\r\n";
                summary += "XML Content: ";
                summary += Translator.objToString(contentMap.get(key)) +"\r\n";
            }
        } catch (Exception e ){ e.printStackTrace(); }
        return summary;
    }

    
    public static void main(String args[]) throws Exception {
        AggregationServer server;
        server = new AggregationServer(1777, "feedInfo.txt");
        server.run();
        server.stop();
    }
}

// Wrapper for boolean so that it can be referenced
class MutableBoolean{
    private boolean mBool;
    public MutableBoolean(){ mBool = false; }
    public MutableBoolean(boolean value){ mBool = value; }
    public boolean getBool(){ return mBool; }
    public void setBool(boolean bool){ mBool = bool; }
}