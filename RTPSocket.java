import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class RTPSocket {
	private static final int WINDOW_SIZE = 10000;
	private static final int MAX_PCKT_SIZE = 1000;
	private static final int BACKLOG_SIZE = 50;

	private static final int FIN = 1 << 0;
	private static final int SYN = 1 << 1;
	private static final int RST = 1 << 2;
	private static final int PSH = 1 << 3;
	private static final int ACK = 1 << 4;
	private static final int URG = 1 << 5;

	private DatagramSocket server_socket;

	private byte[] buffer;
	private DatagramPacket send_pckt;
	private DatagramPacket receive_pckt;

	private ArrayList backlog; //queue of unestablished connections *might not need!
	private HashMap clientMap; //map of all currently connected clients

	private int timeout;
	private boolean closed;


	/**
	 * Creates an unbound RTP server socket
	 * 
	 * @throws IOException if an I/O Error occurs while opening the UDP Socket
	 */
	public RTPSocket() throws IOException {
		this(0,null);
	}

	/**
	 * Creates a RTP server socket and binds it to a specified local port
	 *
	 * @param port    	the port number to use any free port
	 * 
	 * @throws IOException if an I/O Error occurs while opening the UDP Socket
	 */
	public RTPSocket(int port) throws IOException {
		this(port,null);
	}

	/**
	 * Creates a RTP server socket and binds it to a specified local port and IP address
	 *
	 * @param port    	the port number to use any free port
	 * @param bindAddr	the local InetAddress the server will bind to
	 * @throws IOException if an I/O Error occurs while opening the UDP Socket
	 */
	public RTPSocket(int port, InetAddress bindAddr) throws IOException {
		this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
		buffer = new byte[MAX_PCKT_SIZE];
		receive_pckt = new DatagramPacket(buffer, buffer.length);

		backlog = new ArrayList(BACKLOG_SIZE);
		clientMap = new HashMap<Connection>();
		timeout = 0;
		closed = false;
	}

	/**
	 * Binds the server socket to a specified local port and IP.
	 * If IP is null then it will create a socket using the local IP 
	 *
	 * @param port    	the port number to use any free port
	 * 
	 * @throws IOException if an I/O Error occurs while binding the UDP Socket
	 */
	public void bind(int port, InetAddress bindAddr) throws IOException {
		server_socket.bind(new InetSocketAddress(bindAddr, port));
	}

	public void listen() {
		server_socket.receive(receive_pckt);
		int[] header_data = deHeaderize(buffer);
		if((header_data[2] & SYN == 0) && header_data[0] == 0) {
			buffer = headerize(0, 0, WINDOW_SIZE, SYN | ACK, buffer);
			send_pckt = new DatagramPacket(buffer, buffer.length);
			send_pckt.setPort(receive_pckt.getPort());
			send_pckt.setAddress(receive_pckt.getAddress());
			server_socket.send(send_pckt);
		}
	}

	public void connect() {

	}
	
	//TODO: Little confused on the accept method. How are we going to keep the queue going to accept 
	//		the connections? Is it already done for us in the OS or do we have to implement some sort
	//		of backlog? 
	public void accept()  {

	}

	
	/**
     * Register new connection with a client and add to client map.
     *
     * @param endpoint    the new connection.
     * @return the registered client.
     */
	public ClientConnection addClientConnection(SocketAddress endpoint) {
		ClientConnection connection = clientMap.get(endpoint);

		if(connection == null) {
			try {
				connection = new ClientConnection(/*TODO: What will the parameters be?*/);
				clientMap.put(endpoint, connection);
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
     * Removes a client from the clientMap
     *
     * @param endpoint    the connection.
     * @return the removed client.
     */
	public ClientConnection removeClientConnection(SocketAddress endpoint) {
		ClientConnection connection = clientMap.remove(endpoint);

		if(clientMap.isEmpty() && isClosed()) {
			server_socket.close();
		}

		return connection;
	}




	public void receive() {

	}

	public void send() {

	}

	public void close() {

		if(isClosed()) return;

		closed = true;
		backlog.clear();

		if(clientMap.isEmpty()) { server_socket.close(); }
	}

	private int[] deHeaderize(byte[]) {

	}

	private byte[] headerize(int seq_num, int ack_num, int window_size,
							 int flags, byte[] data) {

	}



	public InetAddress getInetAddress()
    {
        return server_socket.getInetAddress();
    }

    public int getLocalPort()
    {
        return server_socket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress()
    {
        return server_socket.getLocalSocketAddress();
    }

    public boolean isBound()
    {
        return server_socket.isBound();
    }

    public boolean isClosed()
    {
        return closed;
    }
}