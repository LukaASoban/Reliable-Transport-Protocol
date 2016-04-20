import java.net.*;
import java.util.HashMap;
import java.util.Arrays;
import java.io.*;

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
    private static final int MAX_PCKT_SIZE = 968;

    private static final long timeout = (long)900;
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
        this.recv_buffer = new byte[10000];
        this.recv_base = 2; // this is the first seq num we should get
        this.send_base = 2; //first seq num we should send
        
        this.isCLOSED = false;
        // RTPStack.createQueue(port);      
        // this.server_socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
        // data_buffer = new byte[MAX_PCKT_SIZE];
        // udp_pckt = new DatagramPacket(data_buffer, data_buffer.length);

        // clientMap = new HashMap<SocketAddress, Connection>();
        // closed = false;
    }
    
    
    public void close() throws InterruptedException {
        
        try{
            if(isCLOSED) return;
            
            RTPacket close = new RTPacket(-1,0,0, new String[]{"FIN"}, null);
            close.setConnectionID(port);
            close.updateChecksum();
            byte[] buf = close.toByteForm();
            DatagramPacket closePacket = new DatagramPacket(buf, buf.length, destAddr, destPort);
            RTPStack.sendQ.put(closePacket);
            
            timelog = System.currentTimeMillis();
            DatagramPacket dgrm_pkt;
            while(true) {

                synchronized(RTPStack.recvQ) {
                    dgrm_pkt = RTPStack.recvQ.get(port).poll();
                }
                
                //is it null?
                if(dgrm_pkt == null) {
                    //System.out.println("Always null..");
                    continue;
                }
                
                //take the data from datagram and convert into RTPacket
                byte[] this_data = dgrm_pkt.getData();
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(this_data);
                String[] flags = rtp_pkt.getFlags();
                if(flags[0].equals("FIN") && flags[4].equals("ACK")) {
                    //System.out.println("fin | ack received..");
                    RTPacket ack = new RTPacket(-1, 0, 0, new String[]{"ACK"}, null);
                    ack.setConnectionID(port);
                    ack.updateChecksum();
                    dgrm_pkt.setData(ack.toByteForm());
                    synchronized(dgrm_pkt) {
                        RTPStack.sendQ.put(dgrm_pkt);
                    }
    
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
                    //System.out.println("RECVQ removed..");
                    synchronized(RTPStack.recvQ) {
                        RTPStack.recvQ.remove(port);
                    }
                    RTPStack.available_ports.add(port);
                    isCLOSED = true;
                    return;
    
                }

                /* THE SYN HAS TIMED OUT*/
                if(System.currentTimeMillis() - timelog > timeout) {
                    RTPStack.sendQ.put(closePacket);
                    timelog = System.currentTimeMillis();
                }
                
            }
        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            System.out.println("recvq closed..");
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        } catch (InterruptedException e) {
            throw e;
        }
        return;
    }


    public void connect(InetAddress serverIP, int serverPort) throws IOException, InterruptedException {
    
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

            System.out.println("Sent a SYN");

            timelog = System.currentTimeMillis();

            while(true) {
                DatagramPacket dgrm_pkt = RTPStack.unestablished.poll();

                /* THE SYN HAS TIMED OUT*/
                if(System.currentTimeMillis() - timelog > timeout) {
                    RTPStack.sendQ.put(synPacket);
                    timelog = System.currentTimeMillis();
                }

                //is the syn corrupt?
                if(dgrm_pkt == null) {
                    continue;
                }

                //take the data from datagram and convert into RTPacket
                byte[] this_data = dgrm_pkt.getData();
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(this_data);
                String[] flags = rtp_pkt.getFlags();
                if(flags[1].equals("SYN") && flags[4].equals("ACK")) {

                    System.out.println("Got a SYN-ACK");

                    //look at the port number - we will always communicate to the server with this port
                    port = rtp_pkt.connectionID();

                    RTPacket ack = new RTPacket(1, 0, 0, new String[]{"ACK"}, null);
                    ack.setConnectionID(port);
                    RTPStack.createQueue(port);
                    ack.updateChecksum();
                    dgrm_pkt.setData(ack.toByteForm());
                    RTPStack.sendQ.put(dgrm_pkt);
                    nextSeqNum = 2;

                    System.out.println("SENT an ACK");

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
            e.printStackTrace();
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            System.out.println("?");
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw ie;
        }

        return;

    }
    
    
    public void accept() throws IOException, InterruptedException {
    
        try {

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }
            DatagramPacket dgrm_pkt;
            //i don't know how to access this particular socket's address in recvqueue
            while(true) {
                synchronized(RTPStack.unestablished) {
                    dgrm_pkt = RTPStack.unestablished.poll();
                }

                if(dgrm_pkt != null) {

                    // Need this data for whenever I make a new SYN ACK in the next while loop
                    this.destAddr = dgrm_pkt.getAddress();
                    this.destPort = dgrm_pkt.getPort();
                    //////////////////////////////////////////

                    byte[] this_data = dgrm_pkt.getData();

                    //take the data from datagram and convert into RTPacket
                    RTPacket rtp_pkt = RTPacket.makeIntoPacket(this_data);

                    String[] flags = rtp_pkt.getFlags();

                    if(flags[1].equals("SYN") && rtp_pkt.seq_num() == 0) {

                        System.out.println("Got a SYN");
                        flags[4] = "ACK";
                        rtp_pkt.setFlags(flags);
                        port = RTPStack.available_ports.remove();
                        rtp_pkt.setConnectionID(port);
                        rtp_pkt.updateChecksum();
                        dgrm_pkt.setData(rtp_pkt.toByteForm());
                        RTPStack.createQueue(port);
                        RTPStack.sendQ.put(dgrm_pkt);

                        System.out.println("Sending a SYN-ACK");
                        break;
                    } 
                }
            }
            timelog = System.currentTimeMillis();
            while(true) {

                dgrm_pkt = RTPStack.recvQ.get(port).poll();

                if(dgrm_pkt != null) {        
                    byte[] this_data = dgrm_pkt.getData();
                    RTPacket rtp_pkt = RTPacket.makeIntoPacket(this_data);
                    String[] flags = rtp_pkt.getFlags();

                    if(flags[4].equals("ACK") && rtp_pkt.seq_num() == 1) {
                        //nextSeqNum = 2;

                        System.out.println("GOT an ACK!!!!");

                        //get the ip and port from where this packet comes from
                        this.destAddr = dgrm_pkt.getAddress();
                        this.destPort = dgrm_pkt.getPort();

                        nextSeqNum = 2; //Must initialize the next sequence number for both client(connect) and server(accept)
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

                    //* Since this is in the if statement only for time we need to send a new SYN ACK with known address*/
                    dgrm_pkt = new DatagramPacket(toSend, toSend.length, destAddr, destPort);
                    // dgrm_pkt.setData(toSend);
                    RTPStack.sendQ.put(dgrm_pkt);

                    timelog = System.currentTimeMillis();
                }
                
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            System.out.println("?");
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            isCLOSED = true;
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw ie;
        }
        return;


    }

    public int receive(byte[] usrBuf, int usrOff, int usrLen) throws IOException, InterruptedException {
    
        try{

            if(isCLOSED) {
                throw new IOException("The socket is already closed!");
            }
            recv_buffer = new byte[10000];
            DatagramPacket dgrm_pkt;
            while(true) {
                //System.out.println(port);
                //get the packet from the recv queue
                dgrm_pkt = RTPStack.recvQ.get(port).poll();

                if(dgrm_pkt == null) {
                    //System.out.println("Always null..");
                    continue;
                }  
                byte[] this_data = dgrm_pkt.getData();
                //take the data from datagram and convert into RTPacket
                RTPacket rtp_pkt = RTPacket.makeIntoPacket(this_data);
                String[] flags = rtp_pkt.getFlags();

                /* CHECK TO SEE IF WE NEED TO SEND AN ACK BACK */
                int seq_num = rtp_pkt.seq_num();
                int flags_num = rtp_pkt.flags();

                //System.out.println("seq_num " + seq_num);
                //System.out.println("flags " + flags_num);
                //System.out.println(Arrays.toString(flags));
                //System.out.println("ack number " + rtp_pkt.getAck());
                if(seq_num >= 2 && (flags_num == 0 || flags[2].equals("RST"))) {
                    if(seq_num == recv_base) {                            

                        while(rtp_pkt != null) {

                            byte[] data = rtp_pkt.getData();
                            if(data.length + recvOffset < recv_buffer.length) {
                                //System.out.println(rtp_pkt.seq_num() + "small enough");
                                System.arraycopy(data, 0, recv_buffer, recvOffset, data.length);
                                recvOffset += data.length;

                                RTPacket ack = new RTPacket(0, rtp_pkt.seq_num(), recvSlidingWnd, new String[]{"ACK"}, null);
                                ack.setConnectionID(port);
                                ack.updateChecksum();
                                byte[] toSend = ack.toByteForm();
                                DatagramPacket ackToSend = new DatagramPacket(toSend, toSend.length, destAddr, destPort);
                                RTPStack.sendQ.put(ackToSend);

                                if(rtp_pkt.getFlags()[2].equals("RST")) {
                                    System.arraycopy(recv_buffer, 0, usrBuf, usrOff, Math.min(usrLen, recv_buffer.length)); // if last packet, copies recvbuffer to userbuffer
                                    RTPacket complete = new RTPacket(0, 8, recvSlidingWnd, new String[]{"PSH"}, null);
                                    complete.setConnectionID(port);
                                    complete.updateChecksum();
                                    byte[] finish = complete.toByteForm();
                                    DatagramPacket completed = new DatagramPacket(finish, finish.length, destAddr, destPort);
                                    RTPStack.sendQ.put(completed);
                                    System.out.println("returning..");
                                    return recvOffset; // returns bytes read
                                }
                                recv_base++;
                                rtp_pkt = inOrderPackets.remove(recv_base);
                            } else {
                                //buffer isn't big enough to hold the packet so we put the datagram back into the recvQ
                                System.out.println(seq_num + "too large");
                                System.arraycopy(recv_buffer, 0, usrBuf, usrOff, Math.min(usrLen, recv_buffer.length));
                                RTPStack.recvQ.get(port).put(dgrm_pkt);
                                int temp = recvOffset;
                                recvOffset =  0;
                                //recv_buffer = new byte[10000];
                                return temp; // returns bytes read
                            }

                        }

                    } else if(seq_num > recv_base && seq_num < recv_base+recvSlidingWnd) {
                        
                        RTPacket ack = new RTPacket(0, seq_num, recvSlidingWnd, new String[]{"ACK"}, null);
                        ack.updateChecksum();
                        ack.setConnectionID(port);
                        byte[] toSend = ack.toByteForm();
                        DatagramPacket ackToSend = new DatagramPacket(toSend, toSend.length, destAddr, destPort);
                        RTPStack.sendQ.put(ackToSend);
                        inOrderPackets.put(seq_num, rtp_pkt);

                    } else {
                        //do nothing
                    }
                }
                
            }

        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection was closed by recipient");
            //delete the recvQ for this socket
            System.out.println("?");
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            e.printStackTrace();
            isCLOSED = true;
        } catch (InterruptedException ie) {
            throw ie;
        }
        return -1;
        


    }

    public void send(byte[] data) throws IOException, InterruptedException {
    
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
                    rtp_pkt = new RTPacket(nextSeqNum, 0, sendSlidingWnd, new String[]{"RST"}, buf);
                    //System.out.println("SeqNum: " + nextSeqNum);
                    //System.out.println("(Pre processing) flag: " + rtp_pkt.flags());
                    //System.out.println(Arrays.toString(rtp_pkt.getFlags()));
                    // rtp_pkt.setConnectionID(port);
                    // rtp_pkt.updateChecksum();
                    // byte[] byteForm = rtp_pkt.toByteForm();
                    // DatagramPacket dp = new DatagramPacket(byteForm, byteForm.length);
                    // send_buffer.add(dp);
                    totalBytesSent += (data.length - totalBytesSent);
                    System.out.println("bytes in packet: " + totalBytesSent);
                } else {
                    byte[] buf = new byte[MAX_PCKT_SIZE];
                    System.arraycopy(data, totalBytesSent, buf, 0, buf.length);
                    rtp_pkt = new RTPacket(nextSeqNum, 0, sendSlidingWnd, new String[]{""}, buf);
                    totalBytesSent += MAX_PCKT_SIZE;
                    System.out.println("(Pre processing) flag: " + rtp_pkt.flags());
                    System.out.println("SeqNum: " + nextSeqNum);
                    //System.out.println(Arrays.toString(rtp_pkt.getFlags()));
                    System.out.println("bytes in packet: " + totalBytesSent);
                }

                rtp_pkt.setConnectionID(port);
                rtp_pkt.updateChecksum();
                byte[] byteForm = rtp_pkt.toByteForm();
                DatagramPacket dp = new DatagramPacket(byteForm, byteForm.length, destAddr, destPort);
                send_buffer.put(nextSeqNum,dp);
                //System.out.println("nextSeqNum = " + nextSeqNum);
                nextSeqNum++;
            }

            //System.out.println(totalBytesSent);

            /* SENDING PACKETS */
            for (int i = send_base; i < Math.min(send_buffer.size() + send_base, sendSlidingWnd + send_base); i++) {
                //System.out.println(send_buffer.size());
                RTPStack.sendQ.put(send_buffer.get(i));
            }

            timelog = System.currentTimeMillis();
            int packets_acked = 0;

            while(true) {

                //get the packet from the recv queue
                DatagramPacket dgrm_pkt = RTPStack.recvQ.get(port).poll();
                
                if(dgrm_pkt != null) {

                    //take the data from datagram and convert into RTPacket
                    rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
                    String[] flags = rtp_pkt.getFlags();

                    /* CHECK TO SEE IF WE NEED TO SEND AN ACK BACK */
                    int ack_num = rtp_pkt.getAck();
                    int flags_num = rtp_pkt.flags();
                    //System.out.println("(Received) flags: " + flags_num);
                    System.out.println("Ack Number: " + ack_num);
                    System.out.println("Send base: " + send_base);
                    //System.out.println(Arrays.toString(flags));

                    if(ack_num != -1) {

                        if(ack_num == send_base) {
                            System.out.println("received ack");

                            while(rtp_pkt != null) {
                                if(send_base + sendSlidingWnd < send_buffer.size() + 2) {
                                    System.out.println("Packet Sent: " + (send_base + sendSlidingWnd));
                                    RTPStack.sendQ.put(send_buffer.get(send_base+sendSlidingWnd));
                                }
                                packets_acked++;
                                send_base++;

                                rtp_pkt = inOrderAcks.remove(send_base);
                            }

                            //reset the sliding window waiting for retrans
                            timelog = System.currentTimeMillis();


                        } else if((ack_num > send_base) && (ack_num < send_base+sendSlidingWnd)) {
                            inOrderAcks.put(ack_num, rtp_pkt);
                            packets_acked++;
                        } else if((ack_num == 8) && (flags[3].equals("PSH"))) {
                            return;
                        } else {
                            //DROP THE PACKET
                        }
                    }
                    System.out.println("packets acked: " + packets_acked);
                    System.out.println("send_buffer: " + send_buffer.size());
                    if(packets_acked >= send_buffer.size()){
                        //System.out.println(send_buffer.size());
                        //System.out.println(send_base);
                        return;
                    }
                }

                /* RE-TRANS FROM TIMEOUT */
                if(System.currentTimeMillis() - timelog > timeout) {
                    for (int i = send_base; i < send_base+sendSlidingWnd; i++) {
                        
                        if(inOrderAcks.get(i) == null && send_buffer.get(i) != null) {
                            //System.out.println(i);
                            RTPStack.sendQ.put(send_buffer.get(i));
                        }
                        timelog = System.currentTimeMillis();

                    }
                }

            }
        } catch (NullPointerException e) {
            if(RTPStack.recvQ.get(port) == null) System.out.println("The connection has already closed");
            //delete the recvQ for this socket
            System.out.println("?");
            RTPStack.recvQ.remove(port);
            RTPStack.available_ports.add(port);
            e.printStackTrace();
            isCLOSED = true;
        } catch (InterruptedException ie) {
            throw ie;
        }
        return;

    }
}