import java.util.concurrent.*;
import java.util.LinkedList;
import java.util.Arrays;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.TimerTask;
import java.util.Timer;

public class RTPStack {
    private byte[] buffer;

    private DatagramSocket socket;
    private DatagramPacket udp_pkt;

    protected static ConcurrentHashMap<Integer, LinkedBlockingQueue<DatagramPacket>> recvQ;
    protected ConcurrentHashMap<Integer, CloseThread> closeThreads;
    protected static LinkedBlockingQueue<DatagramPacket> sendQ;
    protected static LinkedBlockingQueue<DatagramPacket> unestablished;
    protected static LinkedList<Integer> available_ports;
    ExecutorService executor;
    Thread r;
    Thread s;


    public void init(InetAddress bindAddr, int port) throws SocketException{

        try {
            executor = Executors.newFixedThreadPool(1);

            buffer = new byte[1000];
            socket = new DatagramSocket(port);
            socket.setSoTimeout(800);
            recvQ = new ConcurrentHashMap<Integer, LinkedBlockingQueue<DatagramPacket>>();
            sendQ = new LinkedBlockingQueue<DatagramPacket>();
            unestablished = new LinkedBlockingQueue<DatagramPacket>();
            closeThreads = new ConcurrentHashMap<Integer, CloseThread>();
            available_ports = new LinkedList<Integer>();

            for(int i = 1; i < Short.MAX_VALUE; i++) {
                available_ports.add(i);
            }
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            //This is where the recv and send threads will start running
            RecvThread recvthread = new RecvThread();
            SendThread sendthread = new SendThread();
            Thread r = new Thread(recvthread);
            Thread s = new Thread(sendthread);
            r.setDaemon(true);
            s.setDaemon(true);
            
            for (int i = 0; i < 10000; i++) {
                executor.execute(r);
                executor.execute(s);
            }


        } catch (SocketException se) {
            throw se;
        }

    }

    /* This method creates a new queue for the hashmap given a port number*/
    public static void createQueue(int port) {
        LinkedBlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<DatagramPacket>();
        synchronized(recvQ) {
            recvQ.put(port, queue);
        }
    }





    private class RecvThread implements Runnable {

        public void run() {
            

                try{
                    
                    udp_pkt = new DatagramPacket(buffer, buffer.length);
                    socket.receive(udp_pkt); //get a new packet

                    //get the length of the actual packet (buffer - length of packet)
                    byte[] l = new byte[4];
                    System.arraycopy(buffer, 28, l, 0, 4);
                    int lenOfData = RTPacket.byteToInt(l);
                    lenOfData += RTPacket.HEADER_LENGTH;
                    int client_port = udp_pkt.getPort();
                    InetAddress client_addr = udp_pkt.getAddress();

                    //here i set the data to the correct size (buffer to end of rtppacket)
                    l = new byte[lenOfData];
                    //System.out.println(l.length);
                    System.arraycopy(buffer, 0, l, 0, l.length);
                    //udp_pkt.setData(l);
                    DatagramPacket deep_cpy;
                    deep_cpy = new DatagramPacket(l, l.length, client_addr, client_port);


                    //System.out.println(deep_cpy);
                    

                    //if the packet is corrupt, drop the packet
                    //TODO: DELETE isCorrupt FROM EVERY OTHER PART OF THE CODE
                    if(RTPacket.isCorrupt(deep_cpy.getData())) {
                        //System.out.println("corrupt..");
                        return;
                    }

                    
                    //Everytime we get a packet we check its port num to see where the packet 
                    //will go for that particular connection in a recv queue
                    byte[] portOfPacket = new byte[4];
                    System.arraycopy(buffer, 16, portOfPacket, 0, portOfPacket.length);
                    int port_num = RTPacket.byteToInt(portOfPacket);
                    //System.out.println("port " + port_num);

                    
                    //we now check every packet to see if it is a FIN
                    byte[] flags = new byte[4];
                    System.arraycopy(buffer, 0, flags, 0, flags.length);
                    int fin = RTPacket.byteToInt(flags);
                        
                    if((fin & 1) == 1) {
                        if((fin & 16) == 16) {
                            synchronized(recvQ) {
                                recvQ.get(port_num).put(deep_cpy);
                            }
                            //recvQ.get(port_num).put(deep_cpy);
                            return;
                        }
                        //this is the first time we have seen a FIN for this connection
                        if(closeThreads.get(port_num) == null) {
                            RTPacket finack = new RTPacket(-1, 0, 0, new String[]{"FIN","ACK"}, null);
                            //System.out.println("port number = " + port_num);
                            finack.setConnectionID(port_num);
                            finack.updateChecksum();
                            byte[] fin_ack = finack.toByteForm();
                            DatagramPacket finAck = new DatagramPacket(fin_ack, fin_ack.length, udp_pkt.getAddress(), udp_pkt.getPort());
                            //System.out.println("fin|ack sent..");
                            synchronized(sendQ){
                                sendQ.put(finAck);
                            }
                            
                            
                            //We spawn a new thread so that it checks for ACKS               
                            CloseThread ct = new CloseThread(port_num);
                            Thread closer = new Thread(ct);
                            closer.setDaemon(true);
                            closer.start();
                            closeThreads.put(port_num, ct);                
                        }
                    
                        //if we get a FIN from a port that already has a thread running discard it        
                        return;
                    }

                    //is the packet from a established connection or not?
                    synchronized(recvQ) {
                        if(recvQ.get(port_num) == null) {
                            unestablished.put(deep_cpy);
                        } else {
                            //System.out.println("made it to socket..");
                            recvQ.get(port_num).put(deep_cpy);
                        }
                        //buffer = new byte[1000];
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    //e.printStackTrace();
                } finally {

                }

            

        }

    }


    private class SendThread implements Runnable {

        public void run() {
    

                try {
                    DatagramPacket send_pkt;
                    synchronized(sendQ){
                        send_pkt = sendQ.poll();
                    }
                    

                    if(send_pkt != null) {
                        socket.send(send_pkt); // convert to datagram packet
                    }

                } catch (IOException e) {
                    e.printStackTrace();
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
                    DatagramPacket dgm_pkt;
                    synchronized(recvQ) {
                        dgm_pkt = recvQ.get(port).poll();
                    }
                    
                    if(dgm_pkt != null) {
                        RTPacket rtp_pkt = RTPacket.makeIntoPacket(dgm_pkt.getData());
                        String[] flags = rtp_pkt.getFlags();
                        int seq_num = rtp_pkt.seq_num();
                        if(flags[4].equals("ACK") && seq_num == -1) {
                            synchronized(recvQ) {
                                recvQ.remove(port);
                            }
                            this.kill();
                        }
                    }

                    if(System.currentTimeMillis() - timelog > (long)600) {
                        RTPacket finack = new RTPacket(-1, 0, 0, new String[]{"FIN","ACK"}, null);
                        finack.setConnectionID(port);
                        finack.updateChecksum();
                        dgm_pkt.setData(finack.toByteForm());
                        synchronized(sendQ){
                            sendQ.put(dgm_pkt);
                        }
                        
                        timelog = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                    
                } catch (NullPointerException e) {
                    //System.out.println("recvQ is already null..");
                    synchronized(recvQ) {
                        recvQ.remove(port);
                    }
                    this.kill();
                }
            }

        }
            
    }

}