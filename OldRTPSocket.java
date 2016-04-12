import java.net.*;

public class RTPSocket {
    private static final int MAX_PAYLOAD_SIZE = 1000;

    private DatagramSocket socket;
    private DatagramPacket udp_pckt;
    
    private SocketAddress client;

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
        data_buffer = new byte[MAX_PCKT_SIZE];
        udp_pckt = new DatagramPacket(data_buffer, data_buffer.length);

        clientMap = new HashMap<SocketAddress, Connection>();
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
            established.get(client).enqueue(rtp_pckt);      
        }
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
                connection = new ClientConnection(endpoint);
                clientMap.put(endpoint, connection);
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
}