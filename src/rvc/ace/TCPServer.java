package rvc.ace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

class Task{
	String id;
	float complexity;
	Date added;
	
	@Override public String toString()	{
		return id + " " + String.valueOf(complexity)+ " " + added.toString();
	}
	
	public int compareTo(Task other)	{
		return (int) (other.complexity-this.complexity);
	}
}

class TCPServer {

	final static String projectDir = System.getProperty("user.dir");
	
	

	public static void main(String args[]) throws IOException {
		String clientSentence = null;
		ServerSocket welcomeSocket = new ServerSocket(3248);
		
		while (true) {
			Socket connectionSocket = null;
			BufferedOutputStream outToClient = null;
			BufferedReader inFromClient = null;

			connectionSocket = welcomeSocket.accept();
			outToClient = new BufferedOutputStream(connectionSocket.getOutputStream());
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			clientSentence = inFromClient.readLine();
			if (clientSentence != null)
				if (clientSentence.equals("3")) {
					if (outToClient != null) {

						String FileName = "text.java";

						File myFile = new File(projectDir + "\\" + FileName);

						byte[] mybytearray = new byte[(int) myFile.length()];

						FileInputStream fis = null;

						try {
							fis = new FileInputStream(myFile);
						} catch (FileNotFoundException ex) {
							// Do exception handling
						}
						BufferedInputStream bis = new BufferedInputStream(fis);

						try {
							bis.read(mybytearray, 0, mybytearray.length);
							outToClient.write(mybytearray, 0,
									mybytearray.length);
							
							outToClient.flush();
							outToClient.close();
							System.out.println("Finished sending!");

						} catch (IOException ex) {
							// Do exception handling
						}
					}
				} else {
					System.out.println(clientSentence);
					connectionSocket.close();
					welcomeSocket.close();
					//return;
				}
		}
	}
}