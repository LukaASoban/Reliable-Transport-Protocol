import java.util.*;

public class RTPStack {
	
	private DatagramSocket socket;
    private DatagramPacket udp_pckt;

    protected HashMap<Integer, BlockingQueue<DatagramPacket>> recvQ;
    protected BlockingQueue<RTPacket> sendQ;


    public void init(InetAddress bindAddr, int port) {

    	socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
    	recvQ = new HashMap<String, BlockingQueue<DatagramPacket>>();
    	sendQ = new LinkedBlockingQueue<RTPacket>();

    }

    /* This method creates a new queue for the hashmap given a port number*/
    public static createQueue(int port) {
        BlockingQueue<DatagramPacket> queue = new BlockingQueue<DatagramPacket>();
        recvQ.put(port, queue);
    }







    private class RecvThread() implements Runnable{

    	@Override 
    	public void run() {
    		while(true) {

    			try{
    				
    				socket.receive(udp_pkt); //recv
    				packet.source //get the source of hte packet
    				recvQ.get(source).enqueue(udp_pkt); //check if it is in there first



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