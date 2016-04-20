/*
	LUKA ANTOLIC-SOBAN
	RDBA Client

*/
import java.net.*;
import java.io.*;

public class dbclientRTP {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		//checking for valid command line arguments
		if(args.length < 3 || args.length > 7) {
			System.out.println("\nERROR, too few or too many command arguments");
			System.out.println("\nUsage: <ip>:<port> <ID> <column_name>");
			return;
		}

		String[] parts = args[0].split(":");
		String server = parts[0]; //Server IP address

		InetAddress serverAddress = InetAddress.getByName(server);

		int serverPort = Integer.parseInt(parts[1]);

		String query = args[1]; //the string that will be sent to server initialized with GTID

		//Loop through the arguments and grab each column into the bytestream format
		for (int i = 2; i < args.length; i++) {

			query += "," + args[i];

		}


		//converting the query into bytes
		byte[] queryToSend = query.getBytes();
		byte[] querySize = toBytes(queryToSend.length);

		//Creating the socket to connect to the server via TCP

		RTPStack rtp_server = new RTPStack();
		rtp_server.init(null, 0);

		RTPSocket socket = new RTPSocket(null, 0);
		socket.connect(serverAddress,serverPort);

		socket.send(querySize); //First send the file size so that other end knows
		socket.send(queryToSend); //Sending the query to the server

		byte[] receive_buffer = new byte[4]; //find the length of the file to recieve
		int bytesRead = 0;
		while(bytesRead < receive_buffer.length) {
			bytesRead += socket.receive(receive_buffer, bytesRead, receive_buffer.length - bytesRead);
		}

		int sizeOfStream = ((receive_buffer[0] & 0xFF) << 24) | ((receive_buffer[1] & 0xFF) << 16)
        					| ((receive_buffer[2] & 0xFF) << 8) | (receive_buffer[3] & 0xFF);
        	/* END OF GRABBING LENGTH OF QUERY*/

		byte[] queryBuff = new byte[sizeOfStream]; //byte array to hold db query
		bytesRead = 0;
		while(bytesRead < queryBuff.length) {
			bytesRead += socket.receive(queryBuff, bytesRead, queryBuff.length - bytesRead);
		}
	    
	    System.out.println(new String(queryBuff));

		socket.close(); //release resources



	}

	static byte[] toBytes(int i) {
  		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);

  		return result;
	}

}