import java.util.*;
import java.net.*;

public class RTPStack {
	private byte[] buffer;

	private DatagramSocket socket;
    private DatagramPacket udp_pckt;

    protected HashMap<Integer, BlockingQueue<DatagramPacket>> recvQ;
    protected BlockingQueue<DatagramPacket> sendQ;
    protected BlockingQueue<RTPacket> unestablished;
    protected Linkedlist<Integer> available_ports;


    public void init(InetAddress bindAddr, int port) {

        buffer = new byte[1000];
    	socket = new DatagramSocket(new InetSocketAddress(bindAddr, port));
    	recvQ = new HashMap<Integer, BlockingQueue<DatagramPacket>>();
    	sendQ = new LinkedBlockingQueue<DatagramPacket>();
        unestablished =  = new LinkedBlockingQueue<RTPacket>();

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





    private class RecvThread() implements Runnable{

    	@Override 
    	public void run() {
    		while(true) {

    			try{
    				
    				socket.receive(udp_pkt); //get a new packet

                    byte[] portOfPacket = new byte[4];
                    System.arraycopy(buffer, 28, portOfPacket, 0, portOfPacket.length);
                    int port_num = RTPacket.byteToInt(portOfPacket);

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


    private class SendThread() implements Runnable {

    	@Override
    	public void run() {

    		while(true) {

    			DatagramPacket send_pkt = sendQ.take();
    			socket.send(send_pkt); // convert to datagram packet

    		}

    	}
    }


}