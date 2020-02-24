# Distributed News Aggregator
Fault tolerence distributed system written in Java, to comply with ATOM XML and HTTP


## Features Implemented
* HTTP compliant messages passed between nodes (200, 201, 204, 400, 500)
* Semi-featured XML parsing
* Aggregation of Content Server entries into feed
* Thread safe reader-writer locks around resources in memory and disk
* Priority handling of single byte messages as probes
* Adaptive timeouts on clients waiting for responses
* Displaying feed received from server on GetClients
* Fault tolerance on server, will initialise back to the previous checkpoint
* Fault tolerance on content servers, restore cookie and last received hash
* Heartbeat from content serves using HTTP Head requests
* Removal of expired feeds from memory and file after 15 seconds
* Lamport clocks on all classes that are synchronising and updating on message passing
* Requests are queue and worker threads created by Lamport clock priority
* Support for event logging across components of the system
* Support for automated testing scenarios via stoppable threads

## To Do
* Improve XML parsing to reflect full spec (Aggregate aggregates, not entries)
* GetClient Head requests to monitor changes to the feed
* Better handling of socket exceptions (Currently Ignored)

## Changelog
* Support for event logging across components of the system
* Support for automated testing scenarios via stoppable threads
* Added several scenarios that can be run using the instructions below
* Refactored code and converted utility classes to static
* Added a file system lock for thread safe file acess
* Final Spellcheck

## To Run Manually
* Compile all .java files in the assignment2 directory
* Launch separate VM instances for each of the nodes
* java AggregationServer
* Starts the server listening for requests
* java PutClient <content>
* Starts a PutClient that loads the content<x>.txt as its content
* java GetClient
* Starts a client that makes a get request to the server and prints out the received feed

## Automated Testing
* By default all tests will run, comment tests out from the main of TestUtil as required
* Compile all .java files in the assignment2 directory
* java TestUtil
* Runs through the following scenarios:
	- Get request
	- Put without content
	- Put with content and heartbeat
	- Put with sequential Get
	- Multiple Puts then Get
	- Put with Multiple Gets
	- Multiple Puts with Multiple Gets
	- Interleaved Put and Get
	- Feed Expiries due to non-contact
	- Server Restart during uptime
	- Put Client Restart during uptime
	- Put Client failure without restart
* Logs for each of the actors in the distributed system are recorded in Testing Logs
* Each test scenario's logs are stored in a separate folder titled with the test
