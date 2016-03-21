public class RTPacket {
	
	private static final int HEADER_LENGTH = 32; //int x 4 = 32 bytes | data starts after
	private static final int FIN = 1 << 0;
	private static final int SYN = 1 << 1;
	private static final int RST = 1 << 2;
	private static final int PSH = 1 << 3;
	private static final int ACK = 1 << 4;
	private static final int URG = 1 << 5;

	private int length;
	private int seq_num;
	private int ack_num;
	private int retrans_num;
	private int window_size;
	private int flags;
	private byte[] data;

	public RTPacket() {
		retrans_num = 0;
		ack_num = -1;
		data = null;
	}

	public RTPacket(int seq_num, int ack_num, int window_size
					, String[] flags, byte[] data){
		this.seq_num = seq_num;
		this.ack_num = ack_num;
		this.window_size = window_size;

		int setFlag;
		for (String s : flags) {
			if(s.equals("FIN")) { setFlag |= FIN; }
			else if(s.equals("SYN")) { setFlag |= SYN; }
			else if(s.equals("RST")) { setFlag |= RST; }
			else if(s.equals("PSH")) { setFlag |= PSH; }
			else if(s.equals("ACK")) { setFlag |= ACK; } 
			else if(s.equals("URG")) { setFlag |= URG; }
			else { setFlag = 0;}
		}

		this.flags = setFlag;
		this.data = data;
		this.length = data.length + HEADER_LENGTH;
	}

	public int flags(){
		return flags;
	}

	public int seq_num(){
		return seq_num;
	}

	public int window_size(){
		return window_size;
	}

	public int length(){

	}

}