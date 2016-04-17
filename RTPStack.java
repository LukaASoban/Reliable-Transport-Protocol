import java.util.*;
import java.net.*;

public class RTPStack {
    private byte[] buffer;

    private DatagramSocket socket;
    private DatagramPacket udp_pckt;

    protected ConcurrentHashMap<Integer, BlockingQueue<DatagramPacket>> recvQ;
    protected ConcurrentHashMap<Integer, CloseThread> closeThreads;
    protected BlockingQueue<DatagramPacket> sendQ;
    protected BlockingQueue<RTPacket> unestablished;
    protected Linkedlist<Integer> available_ports;


    public void init(InetAddress bindAddr, int port) {

        buffer = new byte[1000];
        socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
        recvQ = new ConcurrentHashMap<Integer, BlockingQueue<DatagramPacket>>();
        sendQ = new LinkedBlockingQueue<DatagramPacket>();
        unestablished = new LinkedBlockingQueue<RTPacket>();
        closeThreads = new ConcurrentHashMap<Integer, CloseThread> closeThreads;

        udp_pkt = new DatagramPacket(buffer, buffer.length);

        for(int i = 1; i < Short.MAX_VAUE; i++) {
            available_ports.add(i);
        }

        //This is where the recv and send threads will start running/////////////////TODO TODO TODO TODO////////////////////////////////////////////////
        RecvThread recvthread = new RecvThread();
        SendThread sendthread = new SendThread();
        new Thread(recvthread).start();
        new Thread(sendthread).start();

    }

    /* This method creates a new queue for the hashmap given a port number*/
    public static createQueue(int port) {
        BlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<DatagramPacket>();
        recvQ.put(port, queue);
    }





    private class RecvThread implements Runnable{

        @Override 
        public void run() {
            while(true) {

                try{
                    
                    socket.receive(udp_pkt); //get a new packet
                    
                    //if the packet is corrupt, drop the packet
                    //TODO: DELETE isCorrupt FROM EVERY OTHER PART OF THE CODE
                    if(RTPacket.isCorrupt(udp_pkt.getData())) {
                        continue;
                    }

                    byte[] portOfPacket = new byte[4];
                    System.arraycopy(buffer, 28, portOfPacket, 0, portOfPacket.length);
                    int port_num = RTPacket.byteToInt(portOfPacket);
                    
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
                                                    
                            CloseThread ct = new CloseThread(port_num);
                            new Thread(ct).start();
                            closeThreads.put(port_num, ct);                
                        }
                    
                                
                        continue;
                    }

                    //is the packet from a established connection or not?
                    if(recvQ.get(port_num) == null) {
                        unestablished.put(udp_pkt);
                    } else {
                        recvQ.get(port_num).put(udp_pkt);
                    }


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }

    }


    private class SendThread implements Runnable {

        @Override
        public void run() {

            while(true) {

                DatagramPacket send_pkt = sendQ.take();
                socket.send(send_pkt); // convert to datagram packet

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

                DatagramPacket dgm_pkt = recvQ.get(port).poll();
                
                if(dgm_pkt != null) {
                    RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgrm_pkt.getData());
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
            }

        }
            
    }

}