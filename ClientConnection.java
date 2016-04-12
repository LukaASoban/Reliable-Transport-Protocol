import java.util.*;

public class ClientConnection {
    private static final int WINDOW_SIZE = 10000; //the number of bytes
    private static Arraylist<byte[]> complete_data;

    private SocketAddress ID;
    public boolean isActive;

    private byte[] read_buffer;
    private byte[] send_buffer;
    
    private HashTable<Integer, RTPacket> in_order;
    
    private ArrayList<RTPacket> recv_queue;
    private ArrayList<RTPacket> send_queue;

    /*TODO: Figure out what a client object will hold*/
    public ClientConnection(SocketAddress ID) {
        this.ID = ID;
        isActive = true;
        recv_queue = new ArrayList<RTPacket>();
    }
    
    public void enqueue(RTPacket p) {
        synchronized(recv_queue){
            for(int i = 0; i < recv_queue.size(); i++) {
                if(recv_queue.get(i).seq_num() == p.seq_num){
                    return;
                }
            }
            recv_queue.add(p);
        }
    }
    
    public RTPacket dequeue() {
        
        
    }
    
    public void manage_receives() {
        RTPacket current = recv_queue.get(0);
        int sequence_num = current.seq_num();
        
        if(in_order.get(sequence_num) == NULL) {
            in_order.put(sequence_num, current);
            String[] flags = current.getFlags();
            if(flags[0].equals("FIN") || recv.queue) {
                readConnection();
                in_order = new RTPacket[5];
            }
            recv_queue.remove(0);
        }
    }
    
    public int getQSize(){
        return recv_queue.size();
    }  
    
    public int readConnection(byte[] buffer) {
    
        int offset = 0;
        for(int i = 0; i < recv_queue; i++) {
            RTPacket rtp = recv_queue.get(i);
            byte[] pckt_data = rtp.getData();
       
            if(offset + pckt_data.length > buffer.length) {
                if(offset <= 0) {
                    throw new IOException("need more buffer space");
                }
                break;
                //System.arraycopy(pckt_data, 0, buffer, offset, buffer.length - offset);
                //return buffer.length;
            }
            System.arraycopy(pckt_data, 0, buffer, offset, pckt_data.length);  
            offset += pckt_data.length;
            recv_buffer.remove(i);
        }
        //recv_buffer.clear();
        return offset;     
    }
    
    
    private class RecieveThread implements Runnable {
    
        public String name;
        
        public RecieveThread(String name) {
            this.name = name;
        }
        
        public void run() {
            
            while(true) {
                
               
                
            }
        }
    
    }
    
}