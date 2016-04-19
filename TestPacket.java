public class TestPacket {
	
	public static void main(String[] args) {
		
		String message = "FUCK THIS";
		byte[] b = message.getBytes();

		RTPacket p = new RTPacket(1, 10, 10, new String[]{"SYN"}, b);

		byte[] packedPacket = p.toByteForm();

		RTPacket newPacket = RTPacket.makeIntoPacket(packedPacket);

		System.out.println(newPacket.getFlags());

	}

}