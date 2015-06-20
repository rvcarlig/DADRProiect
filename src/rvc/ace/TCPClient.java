package rvc.ace;
import java.io.*;
import java.net.*;

class TCPClient {

	final static String projectDir = System.getProperty("user.dir");
	private Socket m_clientSocket = null;
	private InputStream m_inputStream = null;
	private BufferedReader m_inFromClient = null;
    private customDataOutputStream m_outputStream = null;
    private boolean m_busy = false;
	
	public enum clientRequests {
		requestTasksList("1\n"),
		chooseTask("2\n"),
		requestFile("3\n"),
		fileOutput("4\n");
		
		private final String m_request;
		clientRequests(String request){
			m_request=request;
		}
		
		@Override public String toString()
		{
			return m_request;
		}
	};
	
	class customDataOutputStream extends DataOutputStream
	{

		public customDataOutputStream(OutputStream out) {
			super(out);
		}
		
		public void writeBytes(clientRequests request) throws IOException{
			super.writeBytes(request.toString());
		}
	}
	
	public TCPClient(String ip, int port) throws UnknownHostException, IOException
	{
        m_clientSocket = new Socket(ip, port);
	}
	
	public void startListening() throws IOException
	{
        
        m_outputStream = new customDataOutputStream(m_clientSocket.getOutputStream());
        m_inputStream = m_clientSocket.getInputStream();
        m_inFromClient = new BufferedReader(new InputStreamReader(m_inputStream));
        
        while(true)
        {
        	if(!m_busy) // Process requests
        	{
        		
        	}
        	 if (m_inputStream != null) {
        		 
        	 }
        	break;
        }
        
	}
    
    private void getTasksList()
    {
        try {
            m_outputStream.writeBytes(clientRequests.requestTasksList);
        } catch (IOException ex) {
            // Do exception handling
        }
        
        /*TODO
         *Server : for each task : Task Serialize and send byte[] 
         *===== end tasks 
         *deserialize list
         *choose task from list
         */
    }
	
    private void getFile(int id)
    {
        byte[] aByte = new byte[4];

        try {
            m_outputStream.writeBytes(clientRequests.requestFile);
            m_outputStream.writeBytes(String.valueOf(id));
        } catch (IOException ex) {
            // Do exception handling
        }
        if (m_inputStream != null) {

            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {
                fos = new FileOutputStream(projectDir + "\\sss.java");
                bos = new BufferedOutputStream(fos);
                int count;

                while ((count = m_inputStream.read(aByte)) > 0) {
                    bos.write(aByte, 0, count);
                }
                
                bos.flush();
                bos.close();

				System.out.println("Finished receiving!");
            } catch (IOException ex) {
                // Do exception handling
            }
           
        }
    }
    
    private void executeFile(String filename, String extension, String arguments)
    {
    	 //execute file
		try {
			if (extension.equals("java")) {
				Process compile;
				compile = Runtime.getRuntime().exec(
						"javac " + projectDir + "\\" + filename + "."
								+ extension);
				compile.waitFor();
				compile.destroy();
			}
			Process p;
			p = Runtime.getRuntime()
					.exec("java -cp " + projectDir + " " + filename + " "
							+ arguments);

			p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader reade2r = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				// send output
				m_clientSocket = new Socket("127.0.0.1", 3248);
				m_outputStream = new customDataOutputStream(
						m_clientSocket.getOutputStream());
				m_outputStream.writeBytes(line + '\n');
				m_clientSocket.close();
			}
			while ((line = reade2r.readLine()) != null) {
				System.out.println(line);
			}
			p.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    private void sendResult()
    {
    	
    }
    
    public static void main(String args[]) throws IOException {

        TCPClient client = new TCPClient("127.0.0.1", 3248);
        client.startListening();
    }
}