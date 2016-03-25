public class RTPacket {
	
	private static final int HEADER_LENGTH = 20; //int x 5 = 20 bytes | data starts after
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

	public void setWindowSize(int size){
		this.window_size = size;
	}

	public int length(){
		if(data == null) {
			return HEADER_LENGTH;
		}
		return length;
	}

	public void setAck(int ack_num) {
		this.flags = flags | ACK;
		this.ack_num = ack_num;
	}

	public int getAck() {
		if((flags & ACK) == ACK) {
			return ack_num;
		}
		return -1;
	}

	public void setRetransNum(int n){
		this.retrans_num = n;
	}

	public int getRetransNum(){
		return retrans_num;
	}

	public void setData(byte[] buffer, int off, int len) {
		this.data = new byte[len];
		System.arraycopy(buffer, off, data, 0, len);
	}

	public byte[] getData(){
		return data;
	}

	public void setFlags(String[] f) {
		int _flag = 0;
		for (String s : f) {
			if(s.equals("FIN")) { _flag |= FIN; }
			else if(s.equals("SYN")) { _flag |= SYN; }
			else if(s.equals("RST")) { _flag |= RST; }
			else if(s.equals("PSH")) { _flag |= PSH; }
			else if(s.equals("ACK")) { _flag |= ACK; } 
			else if(s.equals("URG")) { _flag |= URG; }
			else { _flag = 0;}
		}

		this.flags = _flag;

	}

	public String[] getFlags() {
		String[] theFlags = new String[6];
		if((flags & FIN) == FIN) {
			theFlags[0] = "FIN";
		}
		if((flags & SYN) == SYN) {
			theFlags[1] = "SYN";
		}
		if((flags & RST) == RST) {
			theFlags[2] = "RST";
		}
		if((flags & PSH) == PSH) {
			theFlags[3] = "PSH";
		}
		if((flags & ACK) == ACK) {
			theFlags[4] = "ACK";
		}
		if((flags & URG) == URG) {
			theFlags[5] = "URG";
		}

		return theFlags;

	}

	public byte[] toByteForm() {

		/*
		 *	flags | len | seq_num | ack_num | window_size
		 */
		byte[] buffer = new byte[length()];
		byte[] b_flags = intToByte(flags);
		byte[] b_header = intToByte(HEADER_LENGTH);
		byte[] b_seq = intToByte(seq_num);
		byte[] b_ack = intToByte(ack_num);
		byte[] b_window = intToByte(window_size);

		for (int i = 0; i < 4; i++) {
			buffer[i] = b_flags[i];
		}
		for (int i = 4; i < 8; i++) {
			buffer[i] = b_header[i];
		}
		for (int i = 8; i < 12; i++) {
			buffer[i] = b_seq[i];
		}
		for (int i = 12; i < 16; i++) {
			buffer[i] = b_ack[i];
		}
		for (int i = 16; i < 20; i++) {
			buffer[i] = b_window[i];
		}

		if(data != null) {
			System.arraycopy(data, 0, buffer, HEADER_LENGTH, data.length);
		}

		return buffer;

		
	}

	public static RTPacket makeIntoPacket(byte[] bytes) {
		byte[] b_flags = new byte[4];
		byte[] b_header = new byte[4];
		byte[] b_seq = new byte[4];
		byte[] b_ack = new byte[4];
		byte[] b_window = new byte[4];
		byte[] b_data = new byte[bytes.length - HEADER_LENGTH];

		RTPacket packet = null;

		for (int i = 0; i < 4; i++) {
			b_flags[i] = bytes[i];
		}
		for (int i = 4; i < 8; i++) {
			b_header[i] = bytes[i];
		}
		for (int i = 8; i < 12; i++) {
			b_seq[i] = bytes[i];
		}
		for (int i = 12; i < 16; i++) {
			b_ack[i] = bytes[i];
		}
		for (int i = 16; i < 20; i++) {
			b_window[i] = bytes[i];
		}


		int _flags = byteToInt(b_flags); 
		int _header = byteToInt(b_header);
		int _seq = byteToInt(b_seq);
		int _ack = byteToInt(b_ack);
		int _window = byteToInt(b_window);
		String[] theFlags = new String[6];
		//check which flags are active
		if((_flags & FIN) == FIN) {
			theFlags[0] = "FIN";
		}
		if((_flags & SYN) == SYN) {
			theFlags[1] = "SYN";
		}
		if((_flags & RST) == RST) {
			theFlags[2] = "RST";
		}
		if((_flags & PSH) == PSH) {
			theFlags[3] = "PSH";
		}
		if((_flags & ACK) == ACK) {
			theFlags[4] = "ACK";
		}
		if((_flags & URG) == URG) {
			theFlags[5] = "URG";
		}


		if(b_data.length == 0) {
			packet = new RTPacket(_seq, _ack, _window, theFlags, null);
		} else {
			System.arraycopy(bytes, HEADER_LENGTH, b_data, 0, b_data.length);
			packet = new RTPacket(_seq, _ack, _window, theFlags, b_data);
		}

		return packet;
	}

	/**
	 * Takes in an integer and converts it to a byte array - BIG ENDIAN
	 *
	 */
	public static  byte[] intToByte(int myInteger){
    	return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
	}

	/**
	 * Takes a byte array and converts it to an integer - BIG ENDIAN
	 *
	 */
	public static int byteToInt(byte [] byteBarray){
    	return ByteBuffer.wrap(byteBarray).order(ByteOrder.BIG_ENDIAN).getInt();
	}



}