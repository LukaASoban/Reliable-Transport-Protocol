import java.net.*;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class fta_server {
	public static void main(String[] args) throws IOException, InterruptedException {
		final int GET = 0;
		final int GET_POST = 17;
		int MAX_RCVWND_SIZE;

		InetAddress server_IP;
		int server_port;
		boolean client_done;

		RTPStack server_socket;
		RTPSocket client_socket;

		if(args.length != 2) {
			throw new IllegalArgumentException("Parameters: <PortNumber> <Max Receive Window>");
		} else {
			server_IP = InetAddress.getByName("127.0.0.1");
			server_port = Integer.parseInt(args[0]);
			MAX_RCVWND_SIZE = Integer.parseInt(args[1]);


			server_socket = new RTPStack();
			server_socket.init(server_IP, server_port);

			while(true) {
				client_socket = new RTPSocket(server_IP, server_port);
				client_socket.accept();
				client_done = false;

				while(!client_done) {

					byte[] request = new byte[5];
					int bytesRead = 0;

					while(bytesRead < request.length) {
						bytesRead += client_socket.receive(request, bytesRead, request.length - bytesRead);
					}

					byte[] filename_length = new byte[4];
					byte[] get_post = new byte[1];

					System.arraycopy(request, 1, filename_length, 0, filename_length.length);

					int get_post_value = request[0];
					int filename_length_value = RTPacket.byteToInt(filename_length);

					byte[] filename = new byte[filename_length_value];
					bytesRead = 0;
					while(bytesRead < filename_length_value) {
						bytesRead += client_socket.receive(filename, bytesRead, filename.length - bytesRead);
						System.out.println(bytesRead);
					}
					System.out.println("received");

					if(get_post_value == GET) {
						String file = new String(filename);
						File filepath = new File(file);
						FileInputStream file_stream = new FileInputStream(filepath);
						byte[] filebytes = IOUtils.toByteArray(file_stream);
						int size_of_file = filebytes.length;

						client_socket.send(RTPacket.intToByte(filebytes.length));
						client_socket.send(filebytes);
						System.out.println("complete");
					} else if(get_post_value == GET_POST) {

					}
					if(client_socket.isClosed()) {
						client_done = true;
					}
				}
			}
		}
	}
}