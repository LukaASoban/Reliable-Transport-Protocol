/*
	LUKA ANTOLIC-SOBAN
	RDBA Client

*/
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class dbclientRTP {
	
	public static void main(String[] args) throws IOException {
		
		//checking for valid command line arguments
		if(args.length < 3 || args.length > 7) {
			System.out.println("\nERROR, too few or too many command arguments");
			System.out.println("\nUsage: <ip>:<port> <ID> <column_name>");
			return;
		}

		String[] parts = args[0].split(":");
		String server = parts[0]; //Server IP address

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

		Socket socket = new Socket(server, serverPort);

		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();

		out.write(querySize); //First send the file size so that other end knows
		out.write(queryToSend); //Sending the query to the server

		/* FIRST GRABBING THE LENGTH OF THE QUERY*/
		int totalBytesRcvd = 0;
		int bytesRcvd;
		byte[] l = new byte[4]; //find the length of the file to recieve
		while(totalBytesRcvd < l.length) {
			if((bytesRcvd = in.read(l, totalBytesRcvd, 
						l.length - totalBytesRcvd)) == -1)
				throw new SocketException("Connection closed prematurely");
			totalBytesRcvd += bytesRcvd;
		}
		int sizeOfStream = ((l[0] & 0xFF) << 24) | ((l[1] & 0xFF) << 16)
    					| ((l[2] & 0xFF) << 8) | (l[3] & 0xFF);
    	/* END OF GRABBING LENGTH OF QUERY*/

		byte[] resultBuff = new byte[sizeOfStream]; //byte array to hold db result

		totalBytesRcvd = 0;		
		while(totalBytesRcvd < resultBuff.length) {
			if((bytesRcvd = in.read(resultBuff, totalBytesRcvd, 
							resultBuff.length - totalBytesRcvd)) == -1)
				throw new SocketException("Connection closed prematurely");
			totalBytesRcvd += bytesRcvd;
		}//the result array is full
	    
	    System.out.println(new String(resultBuff));

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