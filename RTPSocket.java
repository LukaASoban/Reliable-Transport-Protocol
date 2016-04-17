import java.net.*;

public class RTPSocket {

    private boolean isCLOSED;

    private InetAddress bindAddr;
    private int port;

    private InetAddress destAddr;
    private int destPort;

    //recieve buffer
    private byte[] recv_buffer;
    private int recvOffset;
    //send buffer
    private HashMap<Integer, DatagramPacket> send_buffer;
    private int sendOffset;

    private int recv_base;
    private int send_base;
    private int nextSeqNum;
    private int recvSlidingWnd;
    private int sendSlidingWnd;
    private static final int MAX_PCKT_SIZE = 1000;

    private static final long timeout = (long)300;
    private long timelog;

    private HashMap<Integer, RTPacket> inOrderPackets;
    private HashMap<Integer, RTPacket> inOrderAcks;

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
        this.inOrderPackets = new HashMap<Integer, RTPacket>(); //contains sliding window size of packets
        this.inOrderAcks = new HashMap<Integer, RTPacket>();
        this.send_buffer = new HashMap<Integer, DatagramPacket>();
        this.recv_base = 2 // this is the first seq num we should get
        this.send_base = 2; //first seq num we should send
        
        this.isCLOSED = false;
        // RTPStack.createQueue(port);      
        // this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
        // data_buffer = new byte[MAX_PCKT_SIZE];
        // udp_pckt = new DatagramPacket(data_buffer, data_buffer.length);

        // clientMap = new HashMap<SocketAddress, Connection>();
        // closed = false;
    }
    
    
    public void close() {
        
        try{
            if(isCLOSED) return;
            
            RTPacket close = new RTPacket(-1,0,0, new String[]{"FIN"}, null);
            close.updateChecksum();
            byte[] buf = close.toByteForm();
            DatagramPacket closePacket = new DatagramPacket(buf, buf.length, destAddr, destPort);
            RTPStack.sendQ.put(closePacket);
            
            timelog = System.currentTimeMillis();
            
            while(true) {
                
                DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();
                
                /* THE SYN HAS TIMED OUT*/
                if(System.currentTimeMillis() - timelog > timeout) {
                    RTPStack.sendQ.put(closePacket);
                }
                
                //is it null?
                if(dgrm_pkt == null) {
                    continue;
                }
                
                //take the data from datagram and convert into RTPacket
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                String[] flags = rtp_pkt.getFlags();
                if(flags[0].equals("FIN") && flags[4].equals("ACK")) {
    
                    RTPacket ack = new RTPacket(-1, 0, 0, new String[]{"ACK"}, null);
                    ack.setConnectionID(port);
                    ack.updateChecksum();
                    dgrm_pkt.setData(ack.toByteForm());
                    RTPStack.sendQ.put(dgrm_pkt);
    
                    timelog = System.currentTimeMillis();
                    while(System.currentTimeMillis() - timelog < (long)600) {
    
                        if(RTPStack.recvQ.get(port).peek() != null) {
                            break;
                        }
                    }
                    
                    if(RTPStack.recvQ.get(port).peek() != null) {
                        continue;
                    }
                    
                    //delete the recvQ for this socket
                    RTPStack.recvQ.remove(port);
                    RTPStack.available_ports.add(port);
                    isCLOSED = true;
                    return;
    
                }
                
            }
        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        }
        return;
    }


    public void connect(InetAddress serverIP, int serverPort) throws IOException {
    
        try{

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }
        
            this.destAddr = serverIP;
            this.destPort = serverPort;
            
            RTPacket syn = new RTPacket(0, 0, 0, new String[]{"SYN"}, null);
            syn.updateChecksum();
            byte[] buf = syn.toByteForm();
            DatagramPacket synPacket = new DatagramPacket(buf, buf.length, serverIP, serverPort);
            RTPStack.sendQ.put(synPacket);

            timelog = System.currentTimeMillis();

            while(true) {
                DatagramPacket dgrm_pkt = RTPStack.unestablished.poll();

                /* THE SYN HAS TIMED OUT*/
                if(System.currentTimeMillis() - timelog > timeout) {
                    RTPStack.sendQ.put(synPacket);
                }

                //is the syn corrupt?
                if(dgrm_pkt == null || RTPacket.isCorrupt(dgrm_pkt.getData())) {
                    continue;
                }

                //take the data from datagram and convert into RTPacket
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                String[] flags = rtp_pkt.getFlags();
                if(flags[1].equals("SYN") && flags[4].equals("ACK")) {

                    //look at the port number - we will always communicate to the server with this port
                    port = rtp_pkt.connectionID();

                    RTPacket ack = new RTPacket(1, 0, 0, new String[]{"ACK"}, null);
                    ack.setConnectionID(port);
                    ack.updateChecksum();
                    dgrm_pkt.setData(ack.toByteForm());
                    RTPStack.sendQ.put(dgrm_pkt);

                    timelog = System.currentTimeMillis();
                    while(System.currentTimeMillis() - timelog < (long)600) {

                        if(RTPStack.unestablished.peek() != null) {
                            continue;
                        }
                    }

                    return;
                }

            }
        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        }

        return;

    }
    
    
    public void accept() throws IOException {
    
        try {

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }

            //i don't know how to access this particular socket's address in recvqueue
            while(true) {
                DatagramPacket dgrm_pkt = RTPStack.unestablished.poll();

                if(dgrm_pkt != null) {


                    if(RTPacket.isCorrupt(dgrm_pkt.getData())) {
                        continue; //data is corrupt, go and grab the next one
                    }

                    //take the data from datagram and convert into RTPacket
                    RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());

                    String[] flags = rtp_pkt.getFlags();

                    if(flags[1].equals("SYN") && rtp_pkt.seq_num() == 0) {
                        flags[4] = "ACK";
                        rtp_pkt.setFlags(flags);
                        port = RTPStack.available_ports.remove();
                        rtp_pkt.setConnectionID(port));
                        rtp_pkt.updateChecksum();
                        udp_pckt.setData(rtp_pkt.toByteForm());
                        RTPStack.createQueue(port);
                        RTPStack.sendQ.put(udp_pckt);
                        break;
                        //currentWindowSize = rtp_pkt.window_size();
                    } else {
                        //do nothing
                    }
                }
            }

            timelog = System.currentTimeMillis();
            while(true) {
                dgrm_pkt = RTPStack.recvQ.get(port).poll();

                if(dgrm_pkt != null) {

                    if(RTPacket.isCorrupt(dgrm_pkt.getData())) {
                        continue; //data is corrupt, go and grab the next one
                    }                

                    rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                    flags = rtp_pkt.getFlags();

                    if(pckt_flags[4].equals("ACK") && rtp_pkt.seq_num() == 1) {
                        nextSeqNum = 2;

                        //get the ip and port from where this packet comes from
                        this.destAddr = dgrm_pkt.getAddress();
                        this.destPort = dgrm_pkt.getPort();

                        return;
                    } else {
                        //not sure
                    }
                }
                if(System.currentTimeMillis() - timelog > (long)600) {
                    RTPacket synAck = new RTPacket(0, 0, recvSlidingWnd, new String[]{"SYN","ACK"}, null);
                    synAck.setConnectionID(port);
                    synAck.updateChecksum();
                    byte[] toSend = synAck.toByteForm();
                    dgm_pkt.setData(toSend);
                    RTPStack.sendQ.put(dgm_pkt);
                    timelog = System.currentTimeMillis();
                }
                
            }

        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        }
        return;


    }

    public int receive(byte[] usrBuf, int usrOff, int usrLen) throws IOException {
    
        try{

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }

            while(true) {
                //get the packet from the recv queue
                DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();


                if(dgrm_pkt == null || RTPacket.isCorrupt(dgrm_pkt.getData())) {
                    continue;
                }  

                //take the data from datagram and convert into RTPacket
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                String[] flags = rtp_pkt.getFlags();

                /* CHECK TO SEE IF WE NEED TO SEND AN ACK BACK */
                int seq_num = rtp_pkt.seq_num();
                int flags_num = rtp_pkt.flags();

                if(rtp_pkt.seq_num == recv_base) {

                    // we send an ack here if the packet is inside the socket's sliding window
                    if(seq_num >= 2 && (flags == 0) || strFlags[2].equals("RST")) {
                        RTPacket ack = new RTPacket(0, seq_num, recvSlidingWnd, new String[]{"ACK"}, null);
                        ack.setConnectionID(port);
                        ack.updateChecksum();
                        byte[] toSend = ack.toByteForm();
                        DatagramPacket ackToSend = new DatagramPacket(toSend, toSend.length, destAddr, destPort);
                        RTPStack.sendQ.put(ackToSend);
                    }       

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
                    
                    if(seq_num >= 2 && ((flags == 0) || strFlags[2].equals("RST"))) {
                        RTPacket ack = new RTPacket(0, seq_num, recvSlidingWnd, new String[]{"ACK"}, null);
                        ack.updateChecksum();
                        byte[] toSend = ack.toByteForm();
                        DatagramPacket ackToSend = new DatagramPacket(toSend, toSend.length, destAddr, destPort);
                        RTPStack.sendQ.put(ackToSend);
                    }
                    inOrderPackets.put(rtp_pkt.seq_num, rtp_pkt);

                } else {
                    //do nothing
                }


            }

        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        }
        return;
        


    }

    public void send(byte[] data) throws IOException {
    
        try {

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }


            /* PRE-PROCESSING - make the entire byte array into packets*/
            int totalBytesSent = 0;

            RTPacket rtp_pkt;
            while(totalBytesSent < data.length) {
                if(totalBytesSent + MAX_PCKT_SIZE > data.length) {
                    byte[] buf = new byte[data.length - totalBytesSent];
                    System.arraycopy(data, totalBytesSent, buf, 0, buf.length);
                    rtp_pkt = new RTPacket(nextSeqNum, 0, sendSlidingWnd, String[]{"RST"}, buf);
                    // rtp_pkt.setConnectionID(port);
                    // rtp_pkt.updateChecksum();
                    // byte[] byteForm = rtp_pkt.toByteForm();
                    // DatagramPacket dp = new DatagramPacket(byteForm, byteForm.length);
                    // send_buffer.add(dp);
                    totalBytesSent += (data.length - totalBytesSent);
                } else {
                    byte[] buf = new byte[MAX_PCKT_SIZE];
                    System.arraycopy(data, totalBytesSent, buf, 0, buf.length);
                    rtp_pkt = new RTPacket(nextSeqNum, 0, sendSlidingWnd, String[]{""}, buf);
                    totalBytesSent += MAX_PCKT_SIZE;
                }

                rtp_pkt.setConnectionID(port);
                rtp_pkt.updateChecksum();
                byte[] byteForm = rtp_pkt.toByteForm();
                DatagramPacket dp = new DatagramPacket(byteForm, byteForm.length, destAddr, destPort);
                send_buffer.put(nextSeqNum,dp);
                nextSeqNum++;
            }

            /* SENDING PACKETS */
            for (int i = send_base; i < Math.min(send_buffer.size(), sendSlidingWnd); i++) {
                RTPStack.sendQ.put(send_buffer.get(i));
            }

            timelog = System.currentTimeMillis();

            while(true) {

                //get the packet from the recv queue
                DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();
                

                if(dgrm_pkt != null) {

                    //take the data from datagram and convert into RTPacket
                    RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                    String[] flags = rtp_pkt.getFlags();

                    /* CHECK TO SEE IF WE NEED TO SEND AN ACK BACK */
                    int ack_num = rtp_pkt.getAck();
                    int flags_num = rtp_pkt.flags();


                    if(ack_num != -1) {

                        if(ack_num == send_base) {

                            if(send_base >= send_buffer.size()) return;


                            while(rtp_pkt != null) {

                                RTPStack.sendQ.put(send_buffer.get(send_base+sendSlidingWnd));
                                send_base++;

                                rtp_pkt = inOrderAcks.remove(send_base);
                            }

                            //reset the sliding window waiting for retrans
                            timelog = System.currentTimeMillis();


                        } else if(ack_num >= send_base && ack_num < send_base+sendSlidingWnd) {
                            inOrderAcks.put(ack_num, rtp_pkt);
                        } else {
                            //DROP THE PACKET
                        }
                    }

                }

                /* RE-TRANS FROM TIMEOUT */
                if(System.currentTimeMillis() - timelog > timeout) {
                    for (int i = send_base; i < send_base+sendSlidingWnd; i++) {
                        
                        if(inOrderAcks.get(i) == null) {
                            RTPStack.sendQ.put(send_buffer.get(i);
                        }

                    }
                }


            }
        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        }
        return;

    }
}