import java.util.*;
import java.io.*;

public class RTPEngine {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		RTPStack rtpStack = new RTPStack();
		rtpStack.init(null, 8591); // initialize the stack aka the send and recv threads

		System.out.println("Ran once"); 
		

		RTPSocket socket = new RTPSocket(null, 8591);

		//get the client connection
		try{
			socket.accept();
		} catch (Exception e) {
			System.out.println("Error occured in accept");
		}


		while (true) {
			System.out.println("Successfully accepted a connection!");
		}

		// byte[] message = new byte[4];

		// try {
		// 	socket.receive(message, 0, message.length);
		// } catch (Exception e) {
		// 	System.out.println("Error occured in receive");
		// }

		

		// String m = new String(message);

		// System.out.println(m); //we want to get the message called fuck



		// try {
		// 	socket.close();
		// } catch (InterruptedException e) {
		// 	throw e;
		// }
			
		

	}
}