import java.net.*;
import java.io.*;

public class PTServer {

	public static void main(String[] args) throws InterruptedException, IOException {
		RTPStack server_socket;
		RTPSocket client_socket;

		InetAddress server_IP;

		byte[] receive_buffer;
		final String response = "Me neither..";
		server_IP = InetAddress.getByName("127.0.0.1");

		server_socket = new RTPStack();
		server_socket.init(server_IP, 8591);

		while(true) {
			receive_buffer = new byte[480000];

			client_socket = new RTPSocket(server_IP, 8591);
			client_socket.accept();
			System.out.println("Accepted..");
			int bytesRead = 0;
			// client_socket.receive(receive_buffer, 0, receive_buffer.length);
			while(bytesRead < 480000) {
				bytesRead += client_socket.receive(receive_buffer, bytesRead, receive_buffer.length - bytesRead);
				System.out.println(bytesRead);
			}
			System.out.println("Received..");
			System.out.println(new String(receive_buffer));
			client_socket.send(response.getBytes());
			client_socket.close();
			System.out.println("Closed..");
		}
	}
}