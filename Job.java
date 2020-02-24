import java.util.Comparator;
import java.net.Socket;
import java.util.Date;

// Container for request information 
public class Job{
	// Member Variables
	private int mPriority;
	private String mUUID;
	private String mType;
	private Socket mSocket;
	private String mContent;
	private Date mTimeStamp;
	private String mAgentName;

	// Constructors
	public Job(){ mPriority = 1;}
	public Job(String type) { mType = type; }
	public Job(int priority, String type, Socket socket,
										  String content, String agentName)
		{ this (priority, type, socket, content, null, agentName); }
	public Job(int priority, String type, String content,
							 String uuid, Date timeStamp){
		this (priority, type, null, content, uuid, null);
		mTimeStamp = timeStamp;
	}
	public Job(int priority, String type, Socket socket,
							 String content, String uuid, String agentName){
		mType = ((type == null) ? "" : type);
		mAgentName = agentName;
		mPriority = priority;
		mContent = content;
		mSocket = socket;
		mUUID = uuid;
	}
	

	// Accessors
	public String getAgentName(){ return mAgentName; }
	public Date getTimeStamp() { return mTimeStamp; }
	public String getContent(){ return mContent; }
	public int getPriority(){ return mPriority; }
	public Socket getSocket(){ return mSocket; }
	public String getUUID() { return mUUID; }
	public String getType(){ return mType; }
	// Mutators
	public void setAgentName(String agentName){ mAgentName = agentName; }
	public void setTimeStamp(Date timeStamp){ mTimeStamp = timeStamp; }
	public void setPriority(int priority){ mPriority = priority; }
	public void setContent(String content){ mContent = content; }
	public void setSocket(Socket socket){ mSocket = socket; }
	public void setType(String type){ mType = type; }
	public void setUUID(String uuid){ mUUID = uuid; }
}

// Implementation of a comparator for Job queue
class JobComparator implements Comparator<Job>{
	// Enables comparison of jobs based on priority
	public int compare(Job job1, Job job2){
		if (job1.getPriority() < job2.getPriority()){ return 1; }
		else if (job1.getPriority() > job2.getPriority()) { return -1; }
		return 0;
	}
}