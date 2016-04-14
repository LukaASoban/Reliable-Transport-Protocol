import java.util.*;
import java.net.*;

public class RTPStack {
	private byte[] buffer;

	private DatagramSocket socket;
    private DatagramPacket udp_pckt;

    protected HashMap<Integer, BlockingQueue<DatagramPacket>> recvQ;
    protected BlockingQueue<RTPacket> sendQ;
    protected BlockingQueue<RTPacket> unestablished;
    protected Linkedlist<Short> available_ports;


    public void init(InetAddress bindAddr, int port) {

        buffer = new byte[1000];
    	socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
    	recvQ = new HashMap<Integer, BlockingQueue<DatagramPacket>>();
    	sendQ = new LinkedBlockingQueue<RTPacket>();
        unestablished =  = new LinkedBlockingQueue<RTPacket>();

        udp_pkt = new DatagramPacket();

        for(int i = 1; i < Short.MAX_VAUE; i++) {
            available_ports.add(i);
        }

        //This is where the recv and send threads will start running

    }

    /* This method creates a new queue for the hashmap given a port number*/
    public static createQueue(int port) {
        BlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<RTPacket>();
        recvQ.put(port, queue);
    }





    private class RecvThread() implements Runnable{

    	@Override 
    	public void run() {
    		while(true) {

    			try{
    				
    				socket.receive(udp_pkt); //get a new packet
                    


                    //is the packet from a established connection or not?
                    if(recvQ.get(key) == null) {
                        unestablished.put(udp_pkt);
                    } else {
                        recvQ.get(key).put(udp_pkt);
                    }


    			} catch (InterruptedException e) {
    				e.printStackTrace();
    			}

    		}

    	}

    }


    private class SendThread() implements Runnable {

    	@Override
    	public void run() {

    		while(true) {

    			
    			RTPacket send_pkt = sendQ.poll();
    			socket.send(send_pkt); // convert to datagram packet

    		}

    	}
    }


}