public class ClientConnection {

	/*
	 * We might just be able to use the source IP address of the packet instead of an ID
	 * as a key in the HashMap in RTPSocket.java. It could be cleaner.
	*/
	private int ID; 
	private byte[] recv_buffer;
	private byte[] send_buffer;

	/*TODO: Figure out what a client object will hold*/
	public ClientConnection() {

	}


}