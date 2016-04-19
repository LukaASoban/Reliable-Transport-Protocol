public class TestPacket {
	
	

	public static void main(String[] args) {

		long timelog;
		
		String message = "FUCK THIS";
		byte[] b = message.getBytes();

		RTPacket syn = new RTPacket(0, 0, 0, new String[]{"SYN"}, null);
		syn.setConnectionID(455);
		syn.updateChecksum();
		byte[] packedPacket = syn.toByteForm();

		RTPacket newPacket = RTPacket.makeIntoPacket(packedPacket);

		System.out.println(newPacket.connectionID());

		timelog = System.currentTimeMillis();
		while(true) {
			if(System.currentTimeMillis() - timelog > (long)600) {
				System.out.println("send");
				timelog = System.currentTimeMillis();
			}
		}


	}

}