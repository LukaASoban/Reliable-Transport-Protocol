import java.net.*;

public class RTPSocket {

    private InetAddress bindAddr;
    private int port;

    //recieve buffer
    private byte[] recv_buffer;
    private int recvOffset;
    //send buffer
    private byte[] send_buffer;
    private int sendOffset;

    private int recv_base;
    private int send_base;
    private int nextSeqNum;
    private int recvSlidingWnd;
    private int sendSlidingWnd;

    private HashMap<Integer, RTPacket> inOrderPackets;

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

        this.recvSlidingWnd = 5;
        this.sendSlidingWnd = 5;
        this.recv_base = 2 // this is the first seq num we should get
        // RTPStack.createQueue(port);      
        // this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
        // data_buffer = new byte[MAX_PCKT_SIZE];
        // udp_pckt = new DatagramPacket(data_buffer, data_buffer.length);

        // clientMap = new HashMap<SocketAddress, Connection>();
        // closed = false;
    }
	
	
	public void accept() {

        //i don't know how to access this particular socket's address in recvqueue
        while(true) {
            DatagramPacket dgrm_pkt = RTPStack.unestablished.poll();

            if(dgrm_pkt != null) {

                //take the data from datagram and convert into RTPacket
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());

                String[] flags = rtp_pkt.getFlags();

                if(flags[1].equals("SYN") && rtp_pkt.seq_num() == 0) {
                    flags[4] = "ACK";
                    rtp_pkt.setFlags(flags);
                    byte[] data_buffer = rtp_pkt.toByteForm();
                    udp_pckt.setData(data_buffer);
                    port = RTPStack.available_ports.remove();
                    udp_pckt.setConnectionID(port));
                    RTPStack.createQueue(port);
                    RTPStack.sendQ.put(udp_pckt);
                    break;
                    //currentWindowSize = rtp_pkt.window_size();
                } else {
                    //do nothing
                }
            }
        }

        while(true) {
            dgrm_pkt = RTPStack.recvQ.get(port).poll();

            if(dgrm_pkt != null) {
                rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                flags = rtp_pkt.getFlags();

                if(pckt_flags[4].equals("ACK") && rtp_pkt.seq_num() == 1) {
                    nextSeqNum = 2;
                    return;
                } else {
                    //not sure
                }
            }
        }
    }

    public void receive(byte[] usrBuf, int usrOff, int usrLen) throws IOException {

        inOrderPackets = new HashMap<Integer, RTPacket>(); //contains sliding window size of packets

        while(true) {
            //get the packet from the recv queue
            DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).take();

            //take the data from datagram and convert into RTPacket
            RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
            String[] flags = rtp_pkt.getFlags();

            if(rtp_pkt.seq_num == recv_base) {


                while(rtp_pkt != null) {

                    byte[] data = rtp_pkt.getData();
                    if(data.length + recvOffset < recv_buffer.length) {
                        System.arraycopy(data, 0, recv_buffer, recvOffset, data.length);
                        recvOffset += data.length;

                        recv_base += 1;

                        if(flags[2].equals("RST")) {
                            System.arraycopy(recv_buffer, 0, usrBuf, usrOff, Math.min(usrLen, recv_buffer.length)); // if last packet, copies recvbuffer to userbuffer
                            return recvOffset; // returns bytes read
                        }
                    } else {
                        System.arraycopy(recv_buffer, 0, usrBuf, usrOff, Math.min(usrLen, recv_buffer.length));
                        RTPStack.recvQ.get(port).put(rtp_pkt.seq_num, rtp_pkt);
                        int temp = recvOffset;
                        recvOffset =  0;
                        return temp; // returns bytes read
                    }

                    rtp_pkt = inOrderPackets.remove(recv_base);

                }

            } else if(rtp_pkt.seq_num >= recv_base && rtp_pkt.seq_num < recv_base+recvSlidingWnd) {
                inOrderPackets.put(rtp_pkt.seq_num, rtp_pkt);
            } else {
                //do nothing
            }


        }
        







    }
}