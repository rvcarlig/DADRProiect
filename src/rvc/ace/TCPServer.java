package rvc.ace;

import java.io.*;
import java.net.*;



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
				if (clientSentence.equals("1")) {
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
					return;
				}
		}
	}
}