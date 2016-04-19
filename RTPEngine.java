import java.util.*;
import java.io.*;

public class RTPEngine {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		RTPStack rtpStack = new RTPStack();
		rtpStack.init(null, 8591); // initialize the stack aka the send and recv threads
		

		RTPSocket client_socket = new RTPSocket(null, 8591);

		//get the client connection
		try{
			client_socket.accept();
		} catch (Exception e) {
			System.out.println("Error occured in accept");
		}


		
		System.out.println("Successfully accepted a connection!");


		byte[] receive_buffer = new byte[12000];

		System.out.println("Accepted..");
		int bytesRead = 0;
		// client_socket.receive(receive_buffer, 0, receive_buffer.length);
		while(bytesRead < 12000) {
			System.out.println("Here we are");
			bytesRead += client_socket.receive(receive_buffer, bytesRead, receive_buffer.length - bytesRead);
			System.out.println(bytesRead);
		}



		try {
			client_socket.close();
		} catch (InterruptedException e) {
			throw e;
		}
			
		
		System.out.println("We never get here");


	}
}