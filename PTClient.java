import java.net.*;
import java.io.*;

public class PTClient {

	public static void main(String[] args) throws InterruptedException, IOException {
		String message = "";

		for(int i = 0; i < 80000; i++) {
			message += "DDDHGG";
		}

		byte[] response = new byte[12];

		InetAddress client_IP = InetAddress.getByName("127.0.0.1");
		InetAddress server_IP = InetAddress.getByName("192.168.1.215");

		RTPStack client = new RTPStack();
		client.init(client_IP, 9342);

		RTPSocket client_socket = new RTPSocket(client_IP, 9342);

		client_socket.connect(server_IP, 8591);
		System.out.println("Connected...");
		byte[] bytes = message.getBytes();
		System.out.println("Message length: " + bytes.length);
		client_socket.send(bytes);
		System.out.println("Sent..");
		client_socket.receive(response, 0, response.length);
		client_socket.close();

		System.out.println(new String(response));
	}
}