import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.net.InetAddress;
import java.io.IOException;
import java.net.Socket;
// Super class for Put and Get clients
public class Client{
	// Testing flag and event logger
	public static boolean TEST = true;
	protected Logger mLogger;

	protected int mPort;
	protected String mUUID;
	protected int mMaxRetries;
	protected Socket mSocket;
	protected int mProbeTimeout;
	protected boolean mIsStopped;
	protected Socket mProbeSocket;
	protected int mRequestTimeout;
	protected LamportClock mClock;

	public Client(int port, int clock, String uuid){
		// Arbitrary scales of measured latency
		mProbeTimeout = 5 * measureLatency();
		mRequestTimeout = 40 * mProbeTimeout;
		mClock = new LamportClock(clock);
		mMaxRetries = 4;
		mPort = port;
		mUUID = uuid;
	}
	public Client(int port, String uuid)
		{ this(port, 0, uuid); }
	public Client(int port, int clock)
		{ this(port, clock, null); }
	public Client(int port)
		{ this(port, 0, null); }
	public Client(){}

	// Sends a ping to the server and returns if it was successful
	protected Boolean probe(){
		String response;
		try{
			// Connect to the aggregation server
			mProbeSocket = new Socket(InetAddress.getLocalHost(), mPort);
			mProbeSocket.setSoTimeout(mProbeTimeout);
			// Send the request
			sendRequest(mProbeSocket, "1");
			// Wait for response
			response = receiveReply(mProbeSocket);
		} catch (Exception e) { response = "0"; }
		return response.equals("1");
	}

	// Testing method for adaptive timeout
	// Sends a ping to the server and returns if it was successful
	protected Boolean probe(String content){
		String response;
		try{
			// Connect to the aggregation server
			mProbeSocket = new Socket(InetAddress.getLocalHost(), mPort);
			mProbeSocket.setSoTimeout(mProbeTimeout);
			// Send the request
			sendRequest(mProbeSocket, content);
			// Wait for response
			response = adaptiveTimeout(mProbeSocket);
		} catch (Exception e) { response = "0"; }
		return response.equals("1");
	}

	// Measures latency for probe response
	protected int measureLatency(){
		long time = System.nanoTime();
		probe();
		time = System.nanoTime() - time;
		// Convert nanoseconds to milliseconds
		return Math.round((float)(time) / 1000000);
	}

	// Assembles HTTP compliant request messages
	protected String createRequest(String type, String uuid, String content)
												throws UnknownHostException {
		return HTTPHelper.createHttpRequest(type, content,
											this.getClass().getSimpleName(),
													uuid, mClock.getTime());	
	}
	// Overload for get clients without UUID or Content
	protected String createRequest(String type) throws UnknownHostException{
		return HTTPHelper.createHttpRequest(type, "",
										this.getClass().getSimpleName(),
												  "", mClock.getTime());
	}

	// Parses HTTP messages received and returns HTTPResponse objects
	protected HTTPResponse parseHTTPResponse(String httpMessage)
		{ return HTTPHelper.parseHttpResponse(httpMessage); }

	// Sends a service request to the Aggregation server
	// Returns a HTTP response object from the server
	protected HTTPResponse requestService(String type, String xmlContent) {
		HTTPResponse httpResponse = null;
		try{
			// Increment logic clock
			mClock.incrementTime();
			String request = createRequest(type, mUUID, xmlContent);
			// Connect to the aggregation server
			mSocket = new Socket(InetAddress.getLocalHost(), mPort);
			sendRequest(mSocket, request);
			// Wait for response
			String response = adaptiveTimeout(mSocket);
			httpResponse = parseHTTPResponse(response);
			// Adjust logical clock
			mClock.incrementTime(httpResponse.getTime());
		} catch (SocketException c) { /*Socket Closed */ 
		} catch (Exception e) { e.printStackTrace(); }
		return httpResponse;
	}

	protected void sendRequest(Socket socket, String request)
													throws IOException{
		ObjectOutputStream outputStream;
		// Add the semt message to the event log
		if (TEST && mLogger != null) { writeRequestToLog(request); }
		// Send the request
		outputStream = new ObjectOutputStream(socket.getOutputStream());

		outputStream.writeObject(request);
	}

	protected String receiveReply(Socket socket) 
							throws IOException, ClassNotFoundException{
		ObjectInputStream inputStream;
		inputStream = new ObjectInputStream(socket.getInputStream());
		String response = (String) inputStream.readObject();
		// Add the received message to the event log
		if (TEST && mLogger != null) { writeResponseToLog(response); }

		// Close Connection
		socket.close();
		return response;
	}

	// sends a Head request to the Aggregation server
	// Returns a HTTP HEAD response
	protected HTTPResponse heartbeat() 
		{ return requestService("Head",""); }

	// Adaptively sets the response timeout window based on
	// probe requests received between timeouts
	// Returns a raw string of the received response
	// or null if no response is received in the window
	protected String adaptiveTimeout(Socket socket) {
		int timeout = mRequestTimeout;
		String reply = null;
		int retries = 1;

		// Retry mMaxRetries times or until a reply is received
		while (retries < mMaxRetries && reply == null){
			try{
				socket.setSoTimeout(timeout);
				reply = receiveReply(socket);
			} catch (SocketTimeoutException s){
				if(TEST && mLogger != null){
					try{ writeEventToLog("Request Timeout", "Retrying"); }
					catch (Exception e){ e.printStackTrace(); }
				}
				// Extend timeout on successful probe
				timeout = (probe()) ? 2 * timeout : timeout;
				retries++;
			} catch (SocketException c){ /* Socket closed */
			} catch (Exception e){ e.printStackTrace(); }
		}
		return reply;
	}

	public boolean isStopped(){ return mIsStopped; }
	public void stop(){ 
		mIsStopped = true;
		try{
			if (mSocket != null){ mSocket.close();}
			if (mProbeSocket !=null){ mProbeSocket.close(); }
		} catch (Exception e){
			System.out.println("Failed to close Client sockets");
		}
	}

	// Accessors
	public int getPort(){ return mPort; }
	public String getUUID(){ return mUUID; }
	public Logger getLogger(){ return mLogger; }
	public LamportClock getClock(){ return mClock; }

	// Includes an event logger for testing
	public Client(int port, Logger logger){
		this(port);
		mLogger = logger;
	}
	// Writes sent and received messages to the event log
	protected void writeRequestToLog(String message) throws IOException
		{ mLogger.logMessage(true, message); }
	protected void writeResponseToLog(String message) throws IOException
		{ mLogger.logMessage(false, message); }
	protected void writeEventToLog(String event, String details) throws IOException
		{ mLogger.logEvent(event, details); }

}