import static java.nio.file.StandardOpenOption.APPEND;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class Logger{

	private Path mLogPath;
	private int mEventClock;

	// Initialises the directories for logging and creates the path
	// to the log file
	public Logger(String clientName, String testName, String dir){
		String path = "./" + dir + "/" + testName + "/";
		try{ maybeCreateFolder(path); } 
		catch(Exception e) { e.printStackTrace(); }
		mLogPath = Paths.get(path + clientName + ".log");
		mEventClock = 0;
	}

	// Creates a folder for storing logs if on doesn't already exist
	private void maybeCreateFolder(String dir) throws IOException{
		if (!Files.exists(Paths.get(dir)))
			{ Files.createDirectories(Paths.get(dir)); }
	}

	// Appends a string to the log file
	private void writeToLog(String event) throws IOException{
		if (Files.exists(mLogPath))
			{ Files.write(mLogPath, event.getBytes(), APPEND); }
		else{ Files.write(mLogPath, event.getBytes()); }
	}

	// Increments the logical time on the clock
	public synchronized void incrementEvents(){ mEventClock++; }

	// Writes a message sent or received by the client to the log
	public void logMessage(boolean sent, String message) throws IOException{
		String header = "Event " + mEventClock + " "; 
		if (sent) { header += "Message Sent by Client: \r\n \r\n"; }
		else  { header += "Message Received by Client: \r\n \r\n"; }
		header += message + "\r\n \r\n";
		writeToLog(header);		
		incrementEvents();
	}

	// Adds an event to the client log
	public void logEvent(String event, String details) throws IOException{
		String header = "Event " + mEventClock + " ";
		header += event + " occurred on Client: \r\n \r\n";
		header += details + "\r\n \r\n";
		writeToLog(header);
		incrementEvents();
	}



}