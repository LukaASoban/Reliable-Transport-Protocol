import java.util.*;
import java.nio.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class RTPacket {
	
	private static final int HEADER_LENGTH = 28; //int x 5 + long = 28 bytes | data starts after
	private static final int FIN = 1 << 0;
	private static final int SYN = 1 << 1;
	private static final int RST = 1 << 2;
	private static final int PSH = 1 << 3;
	private static final int ACK = 1 << 4;
	private static final int URG = 1 << 5;

	//flags | seq_num | ack_num | window_size | connectionID | checksum

	private int length;
	private int seq_num;
	private int ack_num;
	private int window_size;
	private int flags;
	private int connectionID;
	private long checksum;
	private byte[] data;

	private CRC32 crc32;

	public RTPacket() {
		ack_num = -1;
		data = null;
	}

	public RTPacket(int seq_num, int ack_num, int window_size
					, String[] flags, byte[] data){

		this.seq_num = seq_num;

		if(ack_num != 0) {
			setAck(ack_num);
		} else {
			this.ack_num = -1;
		}

		this.window_size = window_size;

		setFlags(flags);

		if(data == null) {
			this.data = null;
			this.length = HEADER_LENGTH;
		} else {
			this.data = data;
			this.length = data.length + HEADER_LENGTH;
		}

		crc32 = new CRC32();
		this.checksum = 0;

		
	}



	public long updateChecksum() {
		byte[] temp = this.toByteForm();
		crc32.update(temp);
		checksum = crc32.getValue();
		return checksum;
	}

	public static boolean isCorrupt(byte[] data) {
		byte[] tempLong = new byte[8];
		for (int i = 20; i < 28; i++) {
			tempLong[i-20] = data[i];
			data[i] = 0;
		}
		long raw = byteArrayToLong(tempLong);
		CRC32 checker = new CRC32();
		checker.update(data);
		if(checker.getValue() == raw) {
			return false;
		}
		return true;
	}

	public int flags(){
		return flags;
	}

	public int seq_num(){
		return seq_num;
	}

	public void setSeqNum(int n){
		this.seq_num = n;
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
		flags |= ACK;
		this.ack_num = ack_num;
	}

	public int getAck() {
		if((flags & ACK) == ACK) {
			return ack_num;
		}
		return -1;
	}

	public void setData(byte[] buffer, int off, int len) {
		this.data = new byte[len];
		System.arraycopy(buffer, off, data, 0, len);
		this.length = HEADER_LENGTH + data.length;
	}

	public byte[] getData(){
		return data;
	}

	public int connectionID() {
		return this.connectionID;
	}

	public void setConnectionID(int ID) {
		this.connectionID = ID;
	}

	public void setFlags(String[] f) {
		for (String s : f) {
			if(s.equals("FIN")) { flags |= FIN; }
			else if(s.equals("SYN")) { flags |= SYN; }
			else if(s.equals("RST")) { flags |= RST; }
			else if(s.equals("PSH")) { flags |= PSH; }
			else if(s.equals("ACK")) { flags |= ACK; } 
			else if(s.equals("URG")) { flags |= URG; }
		}

	}

	public String[] getFlags() {
		String[] theFlags = new String[]{"","","","","",""};
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
		 *	flags | seq_num | ack_num | window_size | connectionID | checksum
		 */
		byte[] buffer = new byte[length()];
		byte[] b_flags = intToByte(flags);
		byte[] b_seq = intToByte(seq_num);
		byte[] b_ack = intToByte(ack_num);
		byte[] b_window = intToByte(window_size);
		byte[] b_checksum = longToByteArray(checksum);
		byte[] b_ID = intToByte(connectionID);

		for (int i = 0; i < 4; i++) {
			buffer[i] = b_flags[i];
		}
		for (int i = 4; i < 8; i++) {
			buffer[i] = b_seq[i-4];
		}
		for (int i = 8; i < 12; i++) {
			buffer[i] = b_ack[i-8];
		}
		for (int i = 12; i < 16; i++) {
			buffer[i] = b_window[i-12];
		}
		for (int i = 16; i < 20; i++) {
			buffer[i] = b_ID[i-16];
		}
		for (int i = 20; i < 28; i++) {
			buffer[i] = b_checksum[i-20];
		}

		if(data != null) {
			System.arraycopy(data, 0, buffer, HEADER_LENGTH, data.length);
		}

		return buffer;

		
	}

	public static RTPacket makeIntoPacket(byte[] udpBytes) {
		/*
		 *	flags | seq_num | ack_num | window_size | connectionID
		 */
		byte[] b_flags = new byte[4];
		byte[] b_seq = new byte[4];
		byte[] b_ack = new byte[4];
		byte[] b_window = new byte[4];
		byte[] b_ID = new byte[4];
		byte[] b_checksum = new byte[8];
		byte[] b_data = new byte[udpBytes.length - HEADER_LENGTH];

		RTPacket packet = null;

		for (int i = 0; i < 4; i++) {
			b_flags[i] = udpBytes[i];
		}
		for (int i = 4; i < 8; i++) {
			b_seq[i-4] = udpBytes[i];
		}
		for (int i = 8; i < 12; i++) {
			b_ack[i-8] = udpBytes[i];
		}
		for (int i = 12; i < 16; i++) {
			b_window[i-12] = udpBytes[i];
		}
		for (int i = 16; i < 20; i++) {
			b_ID[i-16] = udpBytes[i];
		}
		for (int i = 20; i < 28; i++) {
			b_checksum[i-20] = udpBytes[i];
		}


		int _flags = byteToInt(b_flags);
		int _seq = byteToInt(b_seq);
		int _ack = byteToInt(b_ack);
		int _window = byteToInt(b_window);
		int _ID = byteToInt(b_ID);
		long _checksum = byteArrayToLong(b_checksum);
		String[] theFlags = new String[]{"","","","","",""};
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
			System.arraycopy(udpBytes, HEADER_LENGTH, b_data, 0, b_data.length);
			packet = new RTPacket(_seq, _ack, _window, theFlags, b_data);
		}

		packet.setConnectionID(_ID);
		packet.checksum = _checksum;

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

	//take a byte array and convert to a long
	public static byte[] longToByteArray(long data) {
	    return new byte[] {
	        (byte)((data >> 56) & 0xff),
	        (byte)((data >> 48) & 0xff),
	        (byte)((data >> 40) & 0xff),
	        (byte)((data >> 32) & 0xff),
	        (byte)((data >> 24) & 0xff),
	        (byte)((data >> 16) & 0xff),
	        (byte)((data >> 8 ) & 0xff),
	        (byte)((data >> 0) & 0xff),
	    };
	}

    //take a long and convert to byte array
    public static long byteArrayToLong(byte[] bytes) {
		long l = 0;
		for (int i=0; i<8; i++) {
			l <<= 8;
			l ^= (long) bytes[i] & 0xff;
		}
		return l;
	}


}