import java.net.*;

public class RTPSocket {
	private static final int WINDOW_SIZE = 10000;
	private static final int MAX_PCKT_SIZE = 1000;

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

	public RTPConnection(String type) {
		this.type = type;
		buffer = new byte[MAX_PCKT_SIZE];
		receive_pckt = new DatagramPacket(buffer, buffer.length);
	}

	public void bind(int port_number) {
		server_socket = new DatagramSocket(port_number);
		//server_socket.set
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

	public void accept() {

	}

	public void receive() {

	}

	public void send() {

	}

	public void close() {

	}

	private int[] deHeaderize(byte[]) {

	}

	private byte[] headerize(int seq_num, int ack_num, int window_size,
							 int flags, byte[] data) {

	}
}