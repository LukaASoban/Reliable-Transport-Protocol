import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class RTPSocket {
	private static final int WINDOW_SIZE = 10000;
	private static final int MAX_PCKT_SIZE = 1000;

	private DatagramSocket socket;
	private DatagramPacket udp_pckt;

	private RTPacket rtp_pckt;

	private byte[] data_buffer;
	private String[] pckt_flags;

	private HashMap<SocketAddress, ClientConnection> established; //map of all currently connected clients

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
	 * @param port the port number to use any free port
	 * @throws IOException if an I/O Error occurs while opening the UDP Socket
	 */
	public RTPSocket(int port) throws IOException {
		this(port,null);
	}

	/**
	 * Creates a RTP server socket and binds it to a specified local port and IP address
	 *
	 * @param port the port number to use any free port
	 * @param bindAddr the local InetAddress the server will bind to
	 * @throws IOException if an I/O Error occurs while opening the UDP Socket
	 */
	public RTPSocket(int port, InetAddress bindAddr) throws IOException {
		this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
		buffer = new byte[MAX_PCKT_SIZE];
		udp_pckt = new DatagramPacket(buffer, buffer.length);

		clientMap = new HashMap<Connection>();
		timeout = 0;
		closed = false;
	}

	/**
	 * Binds the server socket to a specified local port and IP.
	 * If IP is null then it will create a socket using the local IP 
	 *
	 * @param port the port number to use any free port
	 * @throws IOException if an I/O Error occurs while binding the UDP Socket
	 */
	public void bind(int port, InetAddress bindAddr) throws IOException {
		server_socket.bind(new InetSocketAddress(bindAddr, port));
	}

	public void listen() {
		SocketAddress client;

		while (true) {
			socket.receive(udp_pckt);
			client = udp_pckt.getSocketAddress();
			rtp_pckt = RTPacket.makeIntoPacket(data_buffer);
			pckt_flags = rtp_pckt.getFlags();

			if(established.get(client) == NULL) {
				if(pckt_flags[1].equals("SYN") && rtp_pckt.seq_num() == 0) {
					pckt_flags[4] = "ACK";
					rtp_pckt.setFlags(pckt_flags);
					data_buffer = rtp_pckt.toByteForm();
					udp_pckt.setData(data_buffer);
					socket.send(udp_pckt);
				} else if(pckt_flags[4].equals("ACK") && rtp_pckt.seq_num() == 1) {
					accept(udp_pckt.getSocketAddress());
				}
			} else {
				if(pckt_flags[4].equals("ACK") && rtp_pckt.seq_num() > 1) {

				}
			}
		}
	}

	public void connect() {

	}
	
	/**
     * Register new connection with a client and add to client map.
     *
     * @param endpoint the new connection.
     * @return the registered client.
     */
	public ClientConnection accept(SocketAddress endpoint) {
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
     * @param endpoint the connection.
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