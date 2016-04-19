import java.util.*;
import java.io.*;
import java.net.*;

public class RTPClient {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		RTPStack rtpStack = new RTPStack();
		rtpStack.init(null, 9000); // 9000 is the client port number

		RTPSocket socket = new RTPSocket(null, 8591);

		InetAddress serverAddress = InetAddress.getByName("127.0.0.1");

		//get the client connection
		try{
			socket.connect(serverAddress, 8591);
		} catch (Exception e) {
			System.out.println("Error occured in connect");
		}

		
		System.out.println("Success in connection");
	

		// String s = "FUCK";
		// byte[] toSend = s.getBytes();

		// try{
		// 	socket.send(toSend);
		// } catch (Exception e) {
		// 	System.out.println("Error occured in send");
		// }

		// try {
		// 	socket.close();
		// } catch (InterruptedException e) {
		// 	throw e;
		// }



	}

}