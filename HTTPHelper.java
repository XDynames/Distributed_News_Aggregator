import java.net.InetAddress;
import java.net.UnknownHostException;

public class HTTPHelper{

	// 			=== PRIVATE METHOD IMPLEMENTATIONS ===
	// Adds HTTP header information for requests to a string
	static private String createRequestHeader(String type, String senderName,
										  String uuid, int logicalTime) 
				 throws IllegalArgumentException, UnknownHostException{
		InetAddress ip = InetAddress.getLocalHost();
		String header = "";
		switch (type) {
			case "Put":  header =  "PUT /atom.xml "; break;
			case "Get":  header =  "GET "; break;
			case "Head": header =  "HEAD "; break;
			default: break;
				//throw new IllegalArgumentException(
					//"Invalid or Unsupported HTTP Request: " + type );
		}
		header +=  "HTTP/1.1\r\n";
        header += "Host: " + ip.getHostName() + "\r\n";
        header += "User-Agent: " + senderName +"\r\n";
        header += "Cookie: " + uuid + "\r\n";
        header += "Server-Timing: " + logicalTime + "\r\n";
        return header;
	} 

	// Adds the xml content being sent to the HTTP message
	static private String addContent(String content, String header){
		header += "\r\n" + content;
 		return header;
	}

	// Add content meta-data to the HTTP message -ETag, Last Modified, ect.
	static private String addMetaInfo(String content, String header){
		// header += "Last-Modified: \r\n";
		header += "ETag: " + content.hashCode() + "\r\n";
		header += "Content-Length: " + content.getBytes().length + "\r\n";
		header += "Content-Type: xml\r\n";
		return header;
	}
	
	// Adds HTTP header information for responses to a string
	static private String createResponseHeader(String senderName, int status,
													 int logicalTime){
		String header = "HTTP/1.1 ";
		switch (status){
			case 200: header += "200 OK\r\n"; break;
			case 201: header += "201 HTTP_CREATED\r\n"; break;
			case 204: header += "204 NO_CONTENT\r\n"; break;
			case 400: header += "400 Bad Request\r\n"; break;
			//case 500: header += "500 Internal Server Error\r\n"; break;
			default:header += "500 Internal Server Error\r\n"; break;
		}
		header += "Server: " + senderName + "\r\n";
		header += "Server-Timing: " + logicalTime + "\r\n";
		return header;
	}


	// 			=== PUBLIC METHOD IMPLEMENTATIONS ===
	/*
		Parses a http request and returns a request object containing
		salient information about the message
		Args: - httpMessage: Received http message as a string
		Return: - request: Object storing relevant request information
	*/
	static public HTTPRequest parseHttpRequest(String httpMessage){
		HTTPRequest request = new HTTPRequest();
		String[] lines = httpMessage.split("\r\n");
		Boolean contentFlag = false;

		// Add information from the message to the http request object
		for (String line : lines){
			String[] words = line.split(" ");
			if (words.length > 1){
				switch (words[0]) {
					case "PUT": request.setType("Put"); break;
					case "GET": request.setType("Get"); break;
					case "HEAD": request.setType("Head"); break;
					case "Cookie:": request.setUUID(words[1]); break;
					case "Content-Length:": contentFlag = true; break;
					case "User-Agent:": 
						request.setAgentName(words[1]); break;
					case "Server-Timing:":
						request.setTime(Integer.valueOf(words[1])); break;
					default: break;
				}
			}
			// If content has been found include it and cease parsing
			if (contentFlag) {
				request.setContent(lines[lines.length -1]);
				break;
			}
		}
		return request;
	}

	/*
		Parses a http response and returns a response object containing
		salient information about the message
		Args: - httpMessage: Received http message as a string
		Return: - response: Object storing relevant response information
	*/
	static public HTTPResponse parseHttpResponse(String httpMessage){
		HTTPResponse response = new HTTPResponse();
		String[] lines = httpMessage.split("\r\n");
		Boolean contentFlag = false;

		// Add information from the message to the http response object
		for (String line : lines){
			String[] words = line.split(" ");
			if (words.length > 1){
				switch (words[0]) {
					case "ETag:": response.setHash(words[1]); break;
					case "Content-Length:": contentFlag = true; break;
					case "Cookie:": response.setUUID(words[1]); break;
					case "HTTP/1.1": response.setStatus(words[1]); break;
					case "Server:": response.setAgentName(words[1]); break;
					case "Server-Timing:":
						response.setTime(Integer.valueOf(words[1])); break;
					default: break;
				}
			}
			// If content has been found include it and cease parsing
			if (contentFlag) {
				response.setContent(lines[lines.length -1]);
				break;
			}
		}
		return response;
	}

	/* 
		Constructs a HTTP compliant request
	 	Args: - type: Request type (Get, Put, Head)
			  - content: data to be sent (XML String)
			  - senderName: name of the requesters class
			  - uuid: Universal Unique Identifier of the sender
			  - logicalTime: Lamport Clock of sender
		Return: A http compliant request as a string
	*/		   
	static public String createHttpRequest(String type, String content,
									String senderName, String uuid,
													int logicalTime)
				throws IllegalArgumentException, UnknownHostException{
		String httpRequest = createRequestHeader(type, senderName,
												 uuid, logicalTime);
		// Add XML content and meta-data to the request
		if (type == "Put") { 
			httpRequest = addMetaInfo(content, httpRequest);
			httpRequest = addContent(content, httpRequest); 
		}
		return httpRequest;
	}

	/* 
		Constructs a HTTP compliant response
	 	Args: - type: Request type being replied to (Get, Put, Head)
			  - content: Data to be sent (XML String)
			  - senderName: Name of the requesters class
			  - status: Code representing the success of the request
			  			(2 - OK, 4 - Bad Request, 5 - Server Error)
			  - logicalTime: Lamport Clock of sender
		Return: A http compliant response as a string
	*/	
	static public String createHttpResponse(String type, String content, 
									 String uuid, String senderName,
									 int status, int logicalTime) {
		String httpResponse = createResponseHeader(senderName, 
												status, logicalTime);
		if (type == "Put" || type == "Head")
			{ httpResponse += "Cookie: " + uuid + "\r\n"; }
		httpResponse = addMetaInfo(content, httpResponse);
		if (status == 200 && type == "Get")
			{ httpResponse = addContent(content, httpResponse); }
		return httpResponse;
	}

	// Overload for responses that have no dependance on type or content
	static public String createHttpResponse(String senderName, int status,
													 int logicalTime){
		String httpResponse = createResponseHeader(senderName, 
												status, logicalTime);
		return httpResponse;
	}
}

// Struct for holding request details parsed
class HTTPRequest{
	private String mType;
	private String mContent;
	private String mAgentName;
	private String mUUID;
	private int mTime;
	// Accessors
	public String getType(){ return mType; }
	public String getContent(){ return mContent; }
	public String getAgentName(){ return mAgentName; }
	public String getUUID(){ return mUUID; }
	public int getTime(){ return mTime; }
	// Mutators
	public void setType(String type){ mType = type; }
	public void setContent(String content){ mContent = content; }
	public void setAgentName(String agentName){ mAgentName = agentName; }
	public void setUUID(String uuid){ mUUID = uuid; }
	public void setTime(int time){ mTime = time; }
}

// Struct for holding response details parsed
class HTTPResponse{
	private String mAgentName;
	private String mContent;
	private String mUUID;
	private int mStatus;
	private int mHash;
	private int mTime;
	// Accessors
	public String getAgentName(){ return mAgentName; }
	public String getContent(){ return mContent; }
	public String getUUID(){ return mUUID; }
	public int getStatus(){ return mStatus; }
	public int getHash(){ return mHash; }
	public int getTime(){ return mTime; }
	// Mutators
	public void setStatus(String status){ mStatus = Integer.parseInt(status); }
	public void setAgentName(String agentName){ mAgentName = agentName; }
	public void setHash(String hash){ mHash = Integer.parseInt(hash); }
	public void setContent(String content){ mContent = content; }
	public void setUUID(String uuid){ mUUID = uuid; }
	public void setTime(int time){ mTime = time; }
}