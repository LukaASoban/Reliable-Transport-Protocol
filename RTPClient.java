import java.util.*;
import java.io.*;
import java.net.*;

public class RTPClient {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		RTPStack rtpStack = new RTPStack();
		rtpStack.init(null, 9000); // 9000 is the client port number

		RTPSocket socket = new RTPSocket(null, 8591);

		InetAddress serverAddress = InetAddress.getByName("192.168.1.215");

		//get the client connection
		socket.connect(serverAddress, 8591);
		System.out.println("Success in connection");
	
		String s = "";

		for (int i = 0; i < 12000; i++) {
			s += "F";
		}

		byte[] toSend = s.getBytes();

		
		socket.send(toSend);

		System.out.println("we sent successfully");
	

		
		socket.close();



	}

}