import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class ClientWorker implements Runnable {
	private RTPSocket client_socket;
	private BlockingQueue<Path> uploadQueue = new ArrayBlockingQueue<>(16);

	public ClientWorker(RTPSocket client) throws IOException {
		this.client_socket = client;
	}

	public void run(){
		
		try {
				boolean client_done = false;

				while(!client_done) {

					byte[] request = new byte[5];
					int bytesRead = 0;

					while(bytesRead < request.length && bytesRead != -1) {
						bytesRead += client_socket.receive(request, bytesRead, request.length - bytesRead);
						System.out.println("stuck");
					}

					byte[] filename_length = new byte[4];
					byte[] get_post = new byte[1];

					System.arraycopy(request, 1, filename_length, 0, filename_length.length);

					int get_post_value = request[0];
					int filename_length_value = RTPacket.byteToInt(filename_length);

					byte[] filename = new byte[filename_length_value];
					bytesRead = 0;
					while(bytesRead < filename_length_value && bytesRead != -1) {
						bytesRead += client_socket.receive(filename, bytesRead, filename.length - bytesRead);
						System.out.println(bytesRead);
					}
					System.out.println("received");

					if(get_post_value == GET) {
						String file = new String(filename);
						System.out.println(file);
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
		} catch(EOFException ex) {
			// connection closed
		} catch(Exception ex) {
			ex.printStackTrace();
			System.out.println("Connection Failed");
		}
	}
	
}