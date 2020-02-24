// Object to marhsall details of content text files for XML creation
import javax.xml.bind.annotation.*;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Date;

@XmlRootElement(name="Content" )
public class Content{
	// Fields
	protected String mTitle;
	protected String mSubtitle;
	protected String mLink;
	protected String mUpdated;
	protected String mAuthor;
	protected String mID;
	protected String mSummary;
	// Accessors
	public String getTitle(){ return mTitle; }
	public String getSubtitle(){ return mSubtitle; }
	public String getLink(){ return mLink; }
	public String getUpdated() { return mUpdated; }
	public String getAuthor() { return mAuthor; }
	public String getID() {return mID; }
	public String getSummary() { return mSummary; }
	// Mutators
	@XmlElement	(name = "title", required = true)
	public void setTitle(String title){ mTitle = title; }
	@XmlElement	(name = "subtitle")
	public void setSubtitle(String subtitle){ mSubtitle = subtitle; }
	@XmlElement	(name = "link")
	public void setLink(String link){ mLink = link; }
	@XmlElement	(name = "updated", required = true)
	public void setUpdated(String updated) { mUpdated = updated; }
	@XmlElement	(name = "author", required = true)
	public void setAuthor(String author){ mAuthor = author; }
	@XmlElement (name ="id", required = true)
	public void setID(String id){ mID = id; }
	@XmlElement (name = "summary")
	public void setSummary(String summary){ mSummary = summary; }

	// Copy imutable values from one content object to another
	public void copy(Content source){
		setTitle(source.getTitle());
		setSubtitle(source.getSubtitle());
		setLink(source.getLink());
		setUpdated(source.getUpdated());
		setAuthor(source.getAuthor());
		setID(source.getID());
		setSummary(source.getSummary());
	}
}

@XmlRootElement(name="ATOMFeed" )
// Object to marhsall details of content text files for XML creation
class ATOMFeed extends Content{
	// Fields
	private ArrayList<Content> mEntries;
	// Accessors
	public ArrayList<Content> getEntries()
	{ return mEntries; }
	//Mutators
	@XmlElement (name = "entries")
	public void setEntries(ArrayList<Content> entries)
	{ mEntries = entries; }

	// Copies imutable objects from one record into another
	public void copy(ATOMFeed source){
		super.copy(source);
		ArrayList<Content> copyEnteries;
		copyEnteries = new ArrayList<Content>();
		for (Content sourceContent: source.getEntries()){
			Content contentCopy = new Content();
			contentCopy.copy(sourceContent);
			copyEnteries.add(contentCopy);
		}
		setEntries(copyEnteries);
	}
}

// Struct for storing client's timing information
class Times{
	private Date mTimeStamp;
	private int mLogicalTime;
	public Times(){}
	public Times(Date timeStamp, int logicalTime){
		mLogicalTime = logicalTime;
		mTimeStamp = timeStamp;
	}
	// Accessors
	public int getTime(){ return mLogicalTime; }
	public Date getTimeStamp(){ return mTimeStamp; }
	// Mutators
	public void setTime(int logicalTime)
		{ mLogicalTime = logicalTime;	}
	public void setTimeStamp(Date timeStamp)
		{ mTimeStamp = timeStamp; }
	// Copies imutable objects from one record into another
	public void copy(Times source){
		setTime(source.getTime());
		setTimeStamp(source.getTimeStamp());
	}
}

// Struct for collecting content server data into the HashMap
class ClientRecord{
    private Date mTimeStamp;
    private int mLogicalTime;
    private Content mContent;

    public ClientRecord(){};
    public ClientRecord(Content content, Date timeStamp,
                                        int logicalTime){
        mLogicalTime = logicalTime;
        mTimeStamp = timeStamp;
        mContent = content;
    }
    public ClientRecord(Content content, Times times){
    	this(content, times.getTimeStamp(), times.getTime());
    }

    public Date getTimeStamp(){ return mTimeStamp; }
    public Content getContent(){ return mContent; }
    public int getTime(){ return mLogicalTime; }

    public void setTimeStamp(Date timeStamp){ mTimeStamp = timeStamp; }
    public void setTime(int logicalTime){ mLogicalTime = logicalTime; }
    public void setContent(Content content){ mContent = content; }

    // Copies imutable values from one record into another
    public void copy(ClientRecord source){
    	getContent().copy(source.getContent());
    	setTime(source.getTime());
    	setTimeStamp(source.getTimeStamp());
    }
}

// Implementation of a comparator for Record queue
class RecordComparator implements Comparator<ClientRecord>{
    // Enables comparison of Records based on Lamport time
    public int compare(ClientRecord rec1, ClientRecord rec2)
    { return Integer.compare(rec2.getTime(), rec1.getTime());  }

}