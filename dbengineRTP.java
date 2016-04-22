/*
	LUKA ANTOLIC-SOBAN
	RDBA Server

*/
import java.net.*;
import java.io.*;
import java.util.Collections; 
import java.util.HashMap; 
import java.util.Arrays;

public class dbengineRTP {

	private static final int BUFFERSIZE = 256;
	private static HashMap<String, Student> database = new HashMap<String, Student>();

	public static void main(String[] args) throws IOException, InterruptedException {


		initialize(); //setup the database

		if(args.length != 1) //the only arguments that we will take in is the port number
			throw new IllegalArgumentException("Please keep input only as: <Port>");

		int portNumber = Integer.parseInt(args[0]);

		//creating a new server socket to accept the client's request for a connection
		RTPStack server = new RTPStack();
		server.init(null, portNumber);

		//size of messages
		int recievedMsgSize;
		byte[] recieveBuffer = new byte[BUFFERSIZE];

		while(true) {

			RTPSocket client_socket = new RTPSocket(null, portNumber);
			client_socket.accept();


			int bytesRecv;
        	byte[] queryBuff = new byte[BUFFERSIZE]; //byte array to hold db query
			int bytesRead = 0;
			client_socket.receive(queryBuff, bytesRead, queryBuff.length - bytesRead);

			System.out.println("got querey");
			String q = new String(queryBuff).trim().replaceAll(" ", "");

			System.out.println(q);

			/* Being to compute the query result to send back to client */
			String[] dbQuery = q.split(",");

			if(database.containsKey(dbQuery[0])) {
				Student student = database.get(dbQuery[0]);

				q = "From server:";
				for (int i = 1; i < (dbQuery.length); i++) {
					if(dbQuery[i].equals("first_name")) {
						q += " first_name: " + student.first_name + ",";
					} else if(dbQuery[i].equals("last_name")) {
						q += " last_name: " + student.last_name + ",";
					} else if(dbQuery[i].equals("quality_points")) {
						q += " quality_points: " + student.quality_points + ",";
					} else if(dbQuery[i].equals("gpa_hours")) {
						q += " gpa_hours: " + student.gpa_hours + ",";
					} else if(dbQuery[i].equals("gpa")) {
						q += " gpa: " + student.gpa + ",";
					} else {
						q = "Error, you have given a wrong column command. Please use the correct column names.";
					}
				}
				if (q.length() > 0 && q.charAt(q.length()-1)==',') {
	     			q = q.substring(0, q.length()-1);
	    		}

			} else {
				q = "ERROR, there is no one in the database under this ID";
			}

			

    		byte[] resultToSend = q.getBytes();

			client_socket.send(resultToSend);

			client_socket.close(); //end the connection

		}


	}









	/* Converts and integer to a byte array to send over stream */
	private static byte[] toBytes(int i) {
  		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);

  		return result;
	}

	/* Method to intialize the database with the required information into a HashMap */
	private static void initialize() {
		Student s1 = new Student("Anthony", "Peterson", 231, 63);
		Student s2 = new Student("Richard", "Harris", 236, 66);
		Student s3 = new Student("Joe", "Miller", 224, 65);
		Student s4 = new Student("Todd", "Collins", 218, 56);
		Student s5 = new Student("Laura", "Stewart", 207, 64);
		Student s6 = new Student("Marie", "Cox", 246, 63);
		Student s7 = new Student("Stephen", "Baker", 234, 66);

		database.put("903076259", s1);
		database.put("903084074", s2);
		database.put("903077650", s3);
		database.put("903083691", s4);
		database.put("903082265", s5);
		database.put("903075951", s6);
		database.put("903084336", s7);
	}



}