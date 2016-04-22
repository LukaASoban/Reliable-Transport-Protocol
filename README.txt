
* ==============================================           
* Group: Luka Antolic-Soban, Tre’Ellis Cooper                   
* email: las3@gatech.edu, EllisCooper@gatech.edu            
* ==============================================            
*                                          
* CS 3251 - Networking I                   
* Programming Assignment 2 - Reliable Transport Protocol
* April 20, 2016                         
*
*
* Included Files:
*      1 Introduction (Reliable Transport Protocol)
*        ------------------------------------------
*		 RTPStack.java - Send/Receive Handler for Socket Instance (Transport Layer Protocol)
*		 RTPSocket.java - Socket Instance of RTP (Transport Layer Protocol)
*		 RTPacket.java -  Datagram Packet Instance of RTP (Transport Layer Protocol)
*      
*    2.1 Relational Database Application
*        -------------------------------
*        dbclientRTP.java - RTP Client Application (Database)
*        dbengineRTP.java - RTP Server Application (Database)
*        Students.java - Information Reference for RTP database
*
*    2.2 File Transfer Application (FTA)
*        -------------------------------
*		 fta_client.java - RTP Client Application (FTA)
*		 fta_server.java - RTP Server Application (FTA)
*        ClientWorker.java - RTP Server Client Multiplexing (FTA)
*
*    	 External Packages and Utility Files
*    	 -----------------------------------
*        package org.apache.commons.io.* - library package that provides many utility functions necessary for the fta application (ie.
* 	     converting File instances to byte array and vise versa).
*
* RTP Client-Server Database Instuctions:
*    1. Navigate a command line instance to the directory containing the RTP files (1 and 2.1).
*	 2. Compile RTP protocol files using the command: "javac RTPStack.java && RTPSocket.java && RTPacker.java"
*    3. Compile RTP client and server applications using the commands: “javac dbclientTCP.java” & “javac dbengineTCP.java” 
*    4. Run dbengineTCP.java first by using the command “java dbengineTCP <PortNumber>”
*    5. The RTP based server will now be running…
*    6. Navigate a second command line instance to the same directory.
*    7. Run dbclientRTP.java next by using the command "java dbclientRTP <ServerIP> <PortNumber> <StudentID> <Query1>..."
*    8. Client queries will be serviced by server database.
*
*
* RTP Client-Server File Transfer Application Instructions:
*    1. Navigate a command line instance to the directory containing the RTP files (1 and 2.2).
*	 2. Compile RTP protocol files using the command: "javac RTPStack.java && RTPSocket.java && RTPacker.java"
*    3. Compile RTP client and server applications using the commands: “javac fta_client.java && fta_server.java” 
*    4. Run fta_server.java first by using the command “java fta_server <PortNumber>”
*    5. The RTP server will now be running…
*    6. Navigate a second command line instance to the same directory.
*    7. Run fta_client.java next by using the command "java fta_client <ServerIP>:<PortNumber>  
*    8. Client will now prompt the user for one of the following commands: get <filename> get-post <filename1> <filename2>
*       disconnect
*    9. Following the get command the requested file will be sent to the user over the internet
*
*
*    NOTE: Server relay database is searched via the StudentID. Search requires a serverIP, Port Number, ID and at least one query.
*    NOTE: Possible <Query> are first_name, last_name, quality_points, gpa_hours, gpa (MAX of 5 queries at once)
*    NOTE: If a non existent ID is searced output will display "search ID not found.."
*    NOTE: If a non-existent attribute is searched, the server will error only for that query and continue processing the other
*    	   valid queries (if any).
*
*    NOTE: The server runs passively until actively terminated. A command such as "cntrl C" will terminate the server 
* 	 	   (both applications).
*	 NOTE: Running the get <filename> command on the fta_client will download the requested file from the fta_server into the current 
*  		   working directory of the client.
*    NOTE: Running the get-post <filename1> <filename2> simultaneously uploads file2 to the server while downloading file1 from the
* 		   server (FTA).
*    NOTE: Port number is binded for the server while the client attempts to pick a local available port.
*
*
* ========================================================================================
* FOR VISUAL SAMPLES OF GENERIC AND/OR SPECIAL-CASE OUTPUT CONSULT sample.txt
*
* FOR THE SPECIFIC RTP DESIGN DOCUMENTATION REFER TO THE PROVIDED FILE: RTPDesign.pdg
* ========================================================================================
*


