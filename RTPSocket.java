import java.net.*;

public class RTPSocket {

    private InetAddress bindAddr;
    private int port;

    private int nextSeqNum;
    private int currentWindowSize;

    private LinkedList<RTPacket> outOfOrderPackets = new LinkedList<RTPacket>();

    /**
     * Creates a RTP server socket and binds it to a specified local port and IP address
     *
     * @param port the port number to use any free port
     * @param bindAddr the local InetAddress the server will bind to
     * @throws IOException if an I/O Error occurs while opening the UDP Socket
     */
    public RTPSocket(InetAddress bindAddr, int port) throws IOException {
        this.bindAddr = bindAddr;
        this.port = port;

        RTPStack.createQueue(port);      
        // this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
        // data_buffer = new byte[MAX_PCKT_SIZE];
        // udp_pckt = new DatagramPacket(data_buffer, data_buffer.length);

        // clientMap = new HashMap<SocketAddress, Connection>();
        // closed = false;
    }
	
	
	public void accept() {

        //i don't know how to access this particular socket's address in recvqueu
        DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();

        //take the data from datagram and convert into RTPacket
        RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());

        String[] flags = rtp_pckt.getFlags();

        if(flags[1].equals("SYN") && rtp_pckt.seq_num() == 0) {
            flags[4] = "ACK";
            rtp_pckt.setFlags(flags);
            byte[] data_buffer = rtp_pckt.toByteForm();
            udp_pckt.setData(data_buffer);
            RTPStack.sendQ.put(udp_pckt);
            currentWindowSize = rtp_pckt.window_size();
        } else {
            //do nothing
        }

        dgrm_pkt = RTPStack.recvQ.get(port).poll();

        rtp_pckt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
        flags = rtp_pckt.getFlags();

        if(pckt_flags[4].equals("ACK") && rtp_pckt.seq_num() == 1) {
            nextSeqNum = 2;
            return;
        } else {
            //not sure
        }


    }

    public void receive() {

        //get the packet from the recv queue
        DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();

        //take the data from datagram and convert into RTPacket
        RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
        String[] flags = rtp_pckt.getFlags();
        currentWindowSize = rtp_pckt.window_size();

        int seqNewPacket = rtp_pckt.seq_num();

        //check if the seq num is the next one we need
        for (int i = 0; i < outOfOrderPackets.size(); i++) {
            
            RTPacket p = outOfOrderPackets.get(i);
            if(p.seq_num() < seqNewPacket) {
                outOfOrderPackets.add(i-1, rtp_pckt);
                break;
            }

        }

        //check if there are any gaps in the linked list
        RTPacket p = outOfOrderPackets.get(0);
        int counter = p.length();
        for (int i = 1; i < outOfOrderPackets.size(); i++) {

            p = outOfOrderPackets.get(i);
            if(counter+1 != p.seq_num) {
                //then we have a gap
                TODO: finish this part where we send doubleacks for each
                gap in the sequence numbers
            }


        }






    }
}