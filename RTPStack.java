import java.util.concurrent.*;
import java.util.LinkedList;
import java.util.Arrays;
import java.net.*;
import java.io.*;

public class RTPStack {
    private byte[] buffer;

    private DatagramSocket socket;
    private DatagramPacket udp_pkt;

    protected static ConcurrentHashMap<Integer, LinkedBlockingQueue<DatagramPacket>> recvQ;
    protected ConcurrentHashMap<Integer, CloseThread> closeThreads;
    protected static LinkedBlockingQueue<DatagramPacket> sendQ;
    protected static LinkedBlockingQueue<DatagramPacket> unestablished;
    protected static LinkedList<Integer> available_ports;


    public void init(InetAddress bindAddr, int port) throws SocketException{

        try {

            buffer = new byte[1000];
            socket = new DatagramSocket(port);
            recvQ = new ConcurrentHashMap<Integer, LinkedBlockingQueue<DatagramPacket>>();
            sendQ = new LinkedBlockingQueue<DatagramPacket>();
            unestablished = new LinkedBlockingQueue<DatagramPacket>();
            closeThreads = new ConcurrentHashMap<Integer, CloseThread>();
            available_ports = new LinkedList<Integer>();

            udp_pkt = new DatagramPacket(buffer, buffer.length);

            for(int i = 1; i < Short.MAX_VALUE; i++) {
                available_ports.add(i);
            }

            //This is where the recv and send threads will start running
            RecvThread recvthread = new RecvThread();
            SendThread sendthread = new SendThread();
            Thread r = new Thread(recvthread);
            Thread s = new Thread(sendthread);
            r.setDaemon(true);
            s.setDaemon(true);
            r.start();
            s.start();


        } catch (SocketException se) {
            throw se;
        }

    }

    /* This method creates a new queue for the hashmap given a port number*/
    public static void createQueue(int port) {
        LinkedBlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<DatagramPacket>();
        recvQ.put(port, queue);
    }





    private class RecvThread implements Runnable {

        @Override 
        public void run() {
            while(true) {

                try{
                    
                    socket.receive(udp_pkt); //get a new packet

                    //get the length of the actual packet (buffer - length of packet)
                    byte[] l = new byte[4];
                    System.arraycopy(buffer, 28, l, 0, 4);
                    int lenOfData = RTPacket.byteToInt(l);
                    lenOfData += RTPacket.HEADER_LENGTH;

                    //here i set the data to the correct size (buffer to end of rtppacket)
                    l = new byte[lenOfData];
                    System.arraycopy(buffer, 0, l, 0, l.length);
                    //udp_pkt.setData(l);
                    DatagramPacket deep_cpy = new DatagramPacket(l, l.length, udp_pkt.getAddress(), udp_pkt.getPort());


                    

                    //if the packet is corrupt, drop the packet
                    //TODO: DELETE isCorrupt FROM EVERY OTHER PART OF THE CODE
                    if(RTPacket.isCorrupt(deep_cpy.getData())) {
                        continue;
                    }

                    
                    //Everytime we get a packet we check its port num to see where the packet 
                    //will go for that particular connection in a recv queue
                    byte[] portOfPacket = new byte[4];
                    System.arraycopy(buffer, 16, portOfPacket, 0, portOfPacket.length);
                    int port_num = RTPacket.byteToInt(portOfPacket);

                    System.out.println("Port: " + port_num + " flags: ");
                    
                    //we now check every packet to see if it is a FIN
                    byte[] flags = new byte[4];
                    System.arraycopy(buffer, 0, flags, 0, flags.length);
                    int fin = RTPacket.byteToInt(flags);
                        
                    if((fin & 1) == 1) {
                        
                        //this is the first time we have seen a FIN for this connection
                        if(closeThreads.get(port_num) == null) {
                            RTPacket finack = new RTPacket(-1, 0, 0, new String[]{"FIN","ACK"}, null);
                            finack.setConnectionID(port_num);
                            finack.updateChecksum();
                            udp_pkt.setData(finack.toByteForm());
                            sendQ.put(udp_pkt);
                            
                            //We spawn a new thread so that it checks for ACKS               
                            CloseThread ct = new CloseThread(port_num);
                            new Thread(ct).start();
                            closeThreads.put(port_num, ct);                
                        }
                    
                        //if we get a FIN from a port that already has a thread running discard it        
                        continue;
                    }

                    //is the packet from a established connection or not?
                    if(recvQ.get(port_num) == null) {
                        unestablished.put(deep_cpy);
                    } else {
                        recvQ.get(port_num).put(deep_cpy);
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new RuntimeException("The udp socket caused an IOException.");
                }

            }

        }

    }


    private class SendThread implements Runnable {

        @Override
        public void run() {

            while(true) {

                try {

                    DatagramPacket send_pkt = sendQ.take();

                    socket.send(send_pkt); // convert to datagram packet

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }
    
    private class CloseThread implements Runnable {
    
        private volatile boolean isRunning = true;
        private int port;
        private long timelog;
        
        public CloseThread(int port) {
            this.port = port;
        }
        
        public void kill() {
           isRunning = false;
        }
        
        
        @Override
        public void run() {
            timelog = System.currentTimeMillis();

            while(isRunning) {

                try{
                    DatagramPacket dgm_pkt = recvQ.get(port).poll();
                    
                    if(dgm_pkt != null) {
                        RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgm_pkt.getData());
                        String[] flags = rtp_pkt.getFlags();
                        int seq_num = rtp_pkt.seq_num();
                        if(flags[4].equals("ACK") && seq_num == -1) {
                            recvQ.remove(port);
                            kill();
                        }
                    }

                    if(System.currentTimeMillis() - timelog > (long)600) {
                        RTPacket finack = new RTPacket(-1, 0, 0, new String[]{"FIN","ACK"}, null);
                        finack.setConnectionID(port);
                        finack.updateChecksum();
                        dgm_pkt.setData(finack.toByteForm());
                        sendQ.put(dgm_pkt);
                        timelog = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
            
    }

}