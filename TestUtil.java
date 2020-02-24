import java.io.IOException;
import java.io.File;

public class TestUtil{

    static public void print(String string){ System.out.println((string)); }
    static public String getDivider()
    { return "==================================================\r\n"; }
    static public String getHeading(String heading){
        String header = getDivider();
        header += "\t\t" + heading + "\r\n";
        header += getDivider();
        return header;
    }

    // Runs the test specified in the documentation
    public static void main(String[] args) throws Exception{
    	TestUtil test = new TestUtil();
        test.deleteMetaFiles();
        test.deleteLogs();
        // Basic Integration Tests
        test.emptyGet();
        test.emptyPut();
        test.putHeart();
        test.putGet();
        test.putPutPutGet();
        test.putPutPutGetGetGet();
        test.interleavedPutGet();
        test.feedExpiry();
        // Fault Tolerance
        test.serverRestart();
        test.putClientRestart();
        test.putClientFailure();
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Get Client requests feed from server
    private void emptyGet() throws IOException{
        printTest1Details();
        sleep(1);
        String testname = "1. Empty-Get";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        GetClient getClient_1 = new GetClient(port, getLog_1);

        Thread tServer = new Thread(server);
        Thread tGetClient_1 = new Thread(getClient_1);
        // Run Test Scenario
        startServer(tServer);
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("1");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client sends request with no content
    private void emptyPut() throws IOException{
        printTest2Details();
        sleep(1);
        String testname = "2. Empty-Put";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "", putLog_1);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(2);
        stopClient(putClient_1, "1");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("2");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client sends request
    // 3. Wait a few seconds for heartbeat messages
    private void putHeart() throws IOException{
        printTest3Details();
        sleep(1);
        String testname = "3. Put-Heartbeat";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "", putLog_1);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        print("Waiting for 6 seconds...");
        sleep(6);
        stopClient(putClient_1, "1");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("3");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client posts content on server
    // 3. Get Client requests feed from server
    private void putGet() throws IOException{
        printTest4Details();
        sleep(1);
        String testname = "4. Put-Get";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        GetClient getClient_1 = new GetClient(port, getLog_1);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tGetClient_1 = new Thread(getClient_1);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopClient(putClient_1, "1");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("4");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. First Put Client posts content on server
    // 3. Second Put Client posts content on server
    // 4. Third Put Client posts content on server
    // 5. Get Client requests feed
    private void putPutPutGet() throws IOException{
        printTest5Details();
        sleep(1);
        String testname = "5. Multiple Put - Get";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        Logger putLog_2 = newLog("PutClient-2", testname);
        Logger putLog_3 = newLog("PutClient-3", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        PutClient putClient_2 = new PutClient(port, "content2", putLog_2);
        PutClient putClient_3 = new PutClient(port, "content3", putLog_3);
        GetClient getClient_1 = new GetClient(port, getLog_1);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tPutClient_2 = new Thread(putClient_2);
        Thread tPutClient_3 = new Thread(putClient_3);
        Thread tGetClient_1 = new Thread(getClient_1);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        startPutClient(tPutClient_2, "2");
        startPutClient(tPutClient_3, "3");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopClient(putClient_1, "1");
        stopClient(putClient_2, "2");
        stopClient(putClient_3, "3");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("5");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Clients post their content on server
    // 3. Get Clients request feed
    private void putPutPutGetGetGet() throws IOException{
        printTest6Details();
        sleep(1);
        String testname = "6. Multiple Put - Multiple Get";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger getLog_3 = newLog("GetClient-3", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        Logger putLog_2 = newLog("PutClient-2", testname);
        Logger putLog_3 = newLog("PutClient-3", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        PutClient putClient_2 = new PutClient(port, "content2", putLog_2);
        PutClient putClient_3 = new PutClient(port, "content3", putLog_3);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);
        GetClient getClient_3 = new GetClient(port, getLog_3);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tPutClient_2 = new Thread(putClient_2);
        Thread tPutClient_3 = new Thread(putClient_3);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        Thread tGetClient_3 = new Thread(getClient_3);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        startPutClient(tPutClient_2, "2");
        startPutClient(tPutClient_3, "3");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        startGetClient(tGetClient_2, "2");
        startGetClient(tGetClient_3, "3");
        sleep(1);
        stopClient(putClient_1, "1");
        stopClient(putClient_2, "2");
        stopClient(putClient_3, "3");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("6");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. 3 Intervealed Put - Gets are performed
    private void interleavedPutGet() throws IOException{
        printTest7Details();
        sleep(1);
        String testname = "7. Interleaved Put - Get";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger getLog_3 = newLog("GetClient-3", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        Logger putLog_2 = newLog("PutClient-2", testname);
        Logger putLog_3 = newLog("PutClient-3", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        PutClient putClient_2 = new PutClient(port, "content2", putLog_2);
        PutClient putClient_3 = new PutClient(port, "content3", putLog_3);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);
        GetClient getClient_3 = new GetClient(port, getLog_3);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tPutClient_2 = new Thread(putClient_2);
        Thread tPutClient_3 = new Thread(putClient_3);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        Thread tGetClient_3 = new Thread(getClient_3);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        startPutClient(tPutClient_2, "2");
        sleep(4);
        startGetClient(tGetClient_2, "1");
        sleep(1);
        startPutClient(tPutClient_3, "3");
        sleep(4);
        startGetClient(tGetClient_3, "3");
        sleep(1);
        stopClient(putClient_1, "1");
        stopClient(putClient_2, "2");
        stopClient(putClient_3, "3");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("7");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client posts content on server
    // 3. Get Client requests feed from server
    // 4. Put Client is stopped and not resumed
    // 5. Get Client request after expiry window (~15s)
    private void feedExpiry() throws IOException{
        printTest8Details();
        sleep(1);
        String testname = "8. Feed Expiry";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopClient(putClient_1, "1");
        print("Waiting for 17 Seconds...");
        sleep(17);
        startGetClient(tGetClient_2, "2");
        sleep(1);
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("8");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client posts content on server
    // 3. Get Client requests feed from server
    // 4. Server process is stopped
    // 5. 1 Second wait
    // 6. Server is restarted
    // 7. Get Client Requests feed from server
    private void serverRestart() throws IOException{
        printTest9Details();
        sleep(1);
        String testname = "9. Server Restart";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopServer(server);
        sleep(5);
        server = new AggregationServer(port, "feedInfo.txt", serverLog);
        tServer = new Thread(server);
        startServer(tServer);
        sleep(1);
        startGetClient(tGetClient_2, "2");
        sleep(1);
        stopClient(putClient_1, "1");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("9");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised
    // 2. Put Client posts content on server
    // 3. Get Client requests feed from server
    // 4. Put Client process is stopped
    // 5. 3 Second wait
    // 6. Put Client is restarted
    // 7. Get Client Requests feed from server
    private void putClientRestart() throws IOException{
        printTest10Details();
        sleep(1);
        String testname = "10. Put Client Restart";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopClient(putClient_1, "1");
        sleep(5);
        putClient_1 = new PutClient(port, "content1", putLog_1);
        tPutClient_1 = new Thread(putClient_1);
        startPutClient(tPutClient_1, "1");
        sleep(1);
        startGetClient(tGetClient_2, "2");
        sleep(1);
        stopClient(putClient_1, "1");
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("10");
    }

    // Tests the order of events:
    // 1. Server is alive and initialised");
    // 2. Put Client posts content on server");
    // 3. Get Client requests feed from server");
    // 4. Put Client process is stopped");
    // 5. 7 Second wait");
    // 7. Get Client Requests feed from server");
    // 8. 10 second wait");
    // 9. Get Client Requests feed from server");
    private void putClientFailure() throws IOException{
        printTest11Details();
        sleep(1);
        String testname = "11. Put Client Failure";
        int port = 1777;
        Logger serverLog = newLog("AggServer", testname);
        Logger getLog_1 = newLog("GetClient-1", testname);
        Logger getLog_2 = newLog("GetClient-2", testname);
        Logger getLog_3 = newLog("GetClient-3", testname);
        Logger putLog_1 = newLog("PutClient-1", testname);
        
        AggregationServer server = new AggregationServer(port, "feedInfo.txt", serverLog);
        PutClient putClient_1 = new PutClient(port, "content1", putLog_1);
        GetClient getClient_1 = new GetClient(port, getLog_1);
        GetClient getClient_2 = new GetClient(port, getLog_2);
        GetClient getClient_3 = new GetClient(port, getLog_3);

        Thread tServer = new Thread(server);
        Thread tPutClient_1 = new Thread(putClient_1);
        Thread tGetClient_1 = new Thread(getClient_1);
        Thread tGetClient_2 = new Thread(getClient_2);
        Thread tGetClient_3 = new Thread(getClient_3);
        // Run Test Scenario
        startServer(tServer);
        startPutClient(tPutClient_1, "1");
        sleep(4);
        startGetClient(tGetClient_1, "1");
        sleep(1);
        stopClient(putClient_1, "1");
        print("Wait for 7 seconds...");
        sleep(7);
        startGetClient(tGetClient_2, "2");
        print("Wait for 10 seconds...");
        sleep(10);
        startGetClient(tGetClient_3, "3");
        sleep(1);
        stopServer(server);
        deleteMetaFiles();
        printTestComplete("11");
    }

    private void printTest1Details(){
        print(getHeading("Test 1: Empty-Get"));
        print("1. Server is alive and initialised");
        print("2. Get Client requests feed from server");
        print(getDivider());
    }

    private void printTest2Details(){
        print(getHeading("Test 2: Empty-Put"));
        print("1. Server is alive and initialised");
        print("2. Put Client sends request with no content");
        print(getDivider());
    }

    private void printTest3Details(){
        print(getHeading("Test 3: Put-Heartbeat"));
        print("1. Server is alive and initialised");
        print("2. Put Client sends request");
        print("3. Wait a few seconds for heartbeat messages");
        print(getDivider());
    }

    private void printTest4Details(){
        print(getHeading("Test 4: Put-Get"));
        print("1. Server is alive and initialised");
        print("2. Put Client posts content on server");
        print("3. Get Client requests feed from server");
        print(getDivider());
    }

    private void printTest5Details(){
        print(getHeading("Test 5: Multiple Put - Get"));
        print("1. Server is alive and initialised");
        print("2. 3 Put Clients posts their content on server");
        print("3. Get Client requests feed from server");
        print(getDivider());
    }

     private void printTest6Details(){
        print(getHeading("Test 6: Multiple Put - Multiple Get"));
        print("1. Server is alive and initialised");
        print("2. 3 Put Clients posts their content on server");
        print("3. 3 Get Clients request the feed from server");
        print(getDivider());
    }

    private void printTest7Details(){
        print(getHeading("Test 7: Interleaved Put - Get"));
        print("1. Server is alive and initialised");
        print("2. 3 Interleaved Put - Gets are performed");
        print(getDivider());
    }

    private void printTest8Details(){
        print(getHeading("Test 8: Feed Expiry"));
        print("1. Server is alive and initialised");
        print("2. Put Client posts content on server");
        print("3. Get Client requests feed from server");
        print("4. Put Client is stopped and not resumed");
        print("5. Get Client request after expiry window (~15s)");
        print(getDivider());
    }

     private void printTest9Details(){
        print(getHeading("Test 9: Server Restart"));
        print("1. Server is alive and initialised");
        print("2. Put Client posts content on server");
        print("3. Get Client requests feed from server");
        print("4. Server process is stopped");
        print("5. 3 Second wait");
        print("6. Server is restarted");
        print("7. Get Client Requests feed from server");
        print(getDivider());
    }

    private void printTest10Details(){
        print(getHeading("Test 10: Put Client Restart"));
        print("1. Server is alive and initialised");
        print("2. Put Client posts content on server");
        print("3. Get Client requests feed from server");
        print("4. Put Client process is stopped");
        print("5. 3 Second wait");
        print("6. Put Client is restarted");
        print("7. Get Client Requests feed from server");
        print(getDivider());
    }

    private void printTest11Details(){
        print(getHeading("Test 11: Put Client Failure"));
        print("1. Server is alive and initialised");
        print("2. Put Client posts content on server");
        print("3. Get Client requests feed from server");
        print("4. Put Client process is stopped");
        print("5. 7 Second wait");
        print("7. Get Client Requests feed from server");
        print("8. 10 second wait");
        print("9. Get Client Requests feed from server");
        print(getDivider());
    }

    private void deleteMetaFiles(){
        File serverFolder = new File("./Server/Checkpoint/");
        File clientFolder = new File("./ContentServer/");
        if (serverFolder.exists()){
            deleteFiles(serverFolder); 
            serverFolder.delete(); 
        }
        if (clientFolder.exists()){ deleteFiles(clientFolder); }
    }

    private void deleteFiles(File folder)
        { for(File file : folder.listFiles()){ file.delete(); } }
    
    private void deleteLogs(){
        File logFolder = new File("./Testing Logs/");
        if (logFolder.exists()){
            for(File folder : logFolder.listFiles()){
                deleteFiles(folder);
                folder.delete();
            }                
        }
    }

    private void printTestComplete(String ref){
        print(getHeading("Test " + ref + " Complete"));
    }

    private Logger newLog(String agentName, String testName)
        { return new Logger(agentName, testName, "Testing Logs"); }

    private void sleep(int seconds){
        try { Thread.sleep(seconds * 1000); }
        catch(Exception e){ e.printStackTrace(); }
    }

    private void startGetClient(Thread getClient, String ref){
        getClient.start();
        print("Get Client-"+ ref + " Started");
    }

    private void startServer(Thread server){
        server.start();
        print("Aggregation Server Started");
    }

    private void startPutClient(Thread putClient, String ref){
        putClient.start();
        print("Put Client-" + ref + " Started");
    }

    private void stopClient(PutClient putClient, String ref){
        putClient.stop();
        print("Stopped Put Client-" + ref);   
    }

    private void stopServer(AggregationServer server){
        server.stop();
        print("Stopped Server");
    }
}