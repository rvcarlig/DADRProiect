package rvc.ace;
import java.io.*;
import java.net.*;

class TCPClient {

	final static String projectDir = System.getProperty("user.dir");
	
	
    public static void main(String args[]) throws IOException {
    	
        byte[] aByte = new byte[4];
        int bytesRead;
        String fileRequest = "1";
        Socket clientSocket = null;
        InputStream is = null;
        DataOutputStream os = null;
        try {
            clientSocket = new Socket("127.0.0.1", 3248);
            os = new DataOutputStream(clientSocket.getOutputStream());
            is = clientSocket.getInputStream();
            os.writeBytes(fileRequest + "\n");
        } catch (IOException ex) {
            // Do exception handling
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (is != null) {

            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                fos = new FileOutputStream(projectDir + "\\sss.java");
                bos = new BufferedOutputStream(fos);
                bytesRead = is.read(aByte, 0, aByte.length);

                int count;

                while ((count = is.read(aByte)) > 0) {
                    bos.write(aByte, 0, count);
                }
                
                bos.flush();
                bos.close();

				System.out.println("Finished receiving!");
            } catch (IOException ex) {
                // Do exception handling
            }
            //execute file
				try {
					Process compile;
					compile = Runtime.getRuntime().exec("javac "+ projectDir + "\\sss.java");
					compile.waitFor();
					compile.destroy();
	            	Process p;
					p = Runtime.getRuntime().exec("java -cp " + projectDir +" sss ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f");
				
            	p.waitFor(); 
            	BufferedReader reader=new BufferedReader( new InputStreamReader(p.getInputStream()) );
            	BufferedReader reade2r=new BufferedReader( new InputStreamReader(p.getErrorStream()) ); 
            	String line; 
            	while((line = reader.readLine()) != null) { 
            		System.out.println(line);         
            		//send output
                    clientSocket = new Socket("127.0.0.1", 3248);
                    os = new DataOutputStream(clientSocket.getOutputStream());
            		os.writeBytes(line + '\n');
                    clientSocket.close();
            	} 
            	while((line = reade2r.readLine()) != null) { 
            		System.out.println(line); 
            	} 
				p.destroy();
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 	
        }
    }
}