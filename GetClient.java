import javax.xml.bind.JAXBException;
import java.util.regex.Pattern;
import java.lang.Runnable;

public class GetClient extends Client implements Runnable{
	public GetClient(int port, int clock, String uuid)
		{ super(port, clock, uuid); }
	public GetClient(int portNumber, int clock)
		{ super(portNumber, clock);	}
	public GetClient(int portNumber)
		{ super(portNumber); }
	public GetClient(int portNumber, Logger logger)
		{ super(portNumber, logger); }

	// Sends a Get request to the Aggregation server
	// Returns a HTTPResponse object containing reply
	private HTTPResponse requestFeed() { 
		HTTPResponse response = null;
		try { response = requestService("Get",""); }
		catch (Exception e) { e.printStackTrace(); }
		return response;
	} 

	private ATOMFeed unmarshalFeed(String xml) {
		ATOMFeed feed = null;
		try { feed = Translator.stringToFeed(xml); }
		catch (Exception e) { e.printStackTrace(); }
		return feed;
	}

	// Sends a Head request to the Aggregation server
	// Returns an code indicating the success or failure of the action
	private int requestFeedUpdate() { return 0; }

	private void print(String string) { System.out.println(wrap(string, 50)); }

	private String printContent(Content content){
		String feedString = "";
		feedString += (content.getTitle() == null) ?
						"" : content.getTitle().toUpperCase() + "\r\n";
		feedString += (content.getSubtitle() == null) ?
						"" : content.getSubtitle()+ "\r\n";
		feedString += (content.getAuthor() == null) ? 
						"" : content.getAuthor() + "\r\n";
		feedString += (content.getLink() == null) ?
						"" : content.getLink() + "\r\n";
		feedString += (content.getUpdated() == null) ?
						"" : content.getUpdated() + "\r\n";
		feedString += (content.getID() == null) ?
						"" : content.getID() + "\r\n";
		feedString += (content.getSummary() == null) ?
						"" : content.getSummary()+"\r\n";
		feedString += "==================================================";
		if (!TEST) { print(feedString); }
		return feedString;
	}
	// Displays aggregated feed to the user
	private void printFeed(ATOMFeed feed) {
		String feedString = printContent((Content) feed) +"\r\n";
		if (feed.getEntries() != null){
			for (Content content : feed.getEntries()){
				feedString += printContent(content) +"\r\n";
			}
		}
		if (TEST && mLogger != null){
			try{ writeEventToLog("Displayed Aggregated Feed", feedString); }
			catch(Exception e){ e.printStackTrace(); }
		}
	}

	public void run() {	
		// Requests an aggregated feed from the server
		HTTPResponse response = requestFeed();
		ATOMFeed feed = unmarshalFeed(response.getContent());	
		// Displays the feed to the user
		printFeed(feed);
	}

	public static void main(String[] args){
		Thread getClient = new Thread(new GetClient(1777));
		getClient.run();
	}


	// ==== LINE WRAPPING CODE SNIPPET FROM STACKOVERFLOW ====
	// https://tinyurl.com/y6mbuu7r
	private static final String LINEBREAK = "\r\n";

	public String wrap(String string, int lineLength) {
	    StringBuilder b = new StringBuilder();
	    for (String line : string.split(Pattern.quote(LINEBREAK))) {
	        b.append(wrapLine(line, lineLength));
	    }
	    return b.toString();
	}

	private String wrapLine(String line, int lineLength) {
	    if (line.length() == 0) return LINEBREAK;
	    if (line.length() <= lineLength) return line + LINEBREAK;
	    String[] words = line.split(" ");
	    StringBuilder allLines = new StringBuilder();
	    StringBuilder trimmedLine = new StringBuilder();
	    for (String word : words) {
	        if (trimmedLine.length() + 1 + word.length() <= lineLength) {
	            trimmedLine.append(word).append(" ");
	        } else {
	            allLines.append(trimmedLine).append(LINEBREAK);
	            trimmedLine = new StringBuilder();
	            trimmedLine.append(word).append(" ");
	        }
	    }
	    if (trimmedLine.length() > 0) {
	        allLines.append(trimmedLine);
	    }
	    allLines.append(LINEBREAK);
	    return allLines.toString();
	}
}