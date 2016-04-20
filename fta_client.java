import org.apache.commons.io.FileUtils;
import java.util.Scanner;
import java.net.*;
import java.io.File;
import java.io.IOException;

public class fta_client {
	public static void main(String[] args) throws IOException, InterruptedException {
		final int GET = 0;
		final int GET_POST = 17;
		int server_port;
		int MAX_RCVWND_SIZE;

		if(args.length != 2) {
			throw new IllegalArgumentException("Parameters: <IPAddress>:<PortNumber> <Max Receive Window>");
		} else {
			MAX_RCVWND_SIZE = Integer.parseInt(args[1]);
			int colon = ':';
			int colon_index = args[0].indexOf(colon);
			server_port = Integer.parseInt(args[0].substring(colon_index + 1));

			boolean done = false;
			Scanner in = new Scanner(System.in);
			String prompt;
			String command;
			String file;

			InetAddress client_IP = InetAddress.getByName("127.0.0.1");
			InetAddress server_IP = InetAddress.getByName(args[0].substring(0, colon_index));

			RTPStack client = new RTPStack();
			client.init(client_IP, 9342);

			RTPSocket client_socket = new RTPSocket(client_IP, 9342);
			client_socket.connect(server_IP, server_port);

			while(!done) {
				System.out.print("Command: ");
				prompt = in.nextLine();
				command = prompt.substring(0, 3);
				file = prompt.substring(4);
				System.out.println(command);
				System.out.println(file);


				if(command.equals("get")) {
					byte[] bytes_getRequest = new byte[5];
					bytes_getRequest[0] = GET;

					byte[] file_length = RTPacket.intToByte(file.length());
					System.out.println(file_length.length);
					System.arraycopy(file_length, 0, bytes_getRequest, 1, file_length.length);
					client_socket.send(bytes_getRequest);

					client_socket.send(file.getBytes());
					System.out.println("sent");

					byte[] length_of_file = new byte[4];
					int bytesRead = 0;
					while(bytesRead < length_of_file.length) {
						bytesRead += client_socket.receive(length_of_file, bytesRead, length_of_file.length - bytesRead);
					}
					System.out.println("received");
					int length = RTPacket.byteToInt(length_of_file);
					byte[] size = new byte[length];
					bytesRead = 0;
					while(bytesRead < length) {
						bytesRead += client_socket.receive(size, bytesRead, size.length - bytesRead);
					}
					FileUtils.writeByteArrayToFile(new File("file.txt"), size);
				} else if(prompt.equals("disconnect")) {
					client_socket.close();
					done = true;
				}
			}
		}
	}
}