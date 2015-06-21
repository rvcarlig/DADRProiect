package rvc.ace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Task{
	String id;
	float complexity;
	Date added;
	
	public Task() {
		
	}
	@Override public String toString()	{
		return id + " " + String.valueOf(complexity)+ " " + added.toString();
	}
	
	public int compareTo(Task other)	{
		return (int) (other.complexity-this.complexity);
	}
}

class TCPServer {

	final static String projectDir = System.getProperty("user.dir");
	private ServerSocket welcomeSocket = null;
	private Map<String, String> tasksMap = new HashMap<String, String>();
	private List<Task> tasksList = new ArrayList<Task>();
	
	public enum serverResponses {
		tasksAvailable("1\n"), tasksUnavailable("2\n");

		private final String m_response;

		serverResponses(String response) {
			m_response = response;
		}

		@Override
		public String toString() {
			return m_response;
		}
	};
	
	public TCPServer() {
		try {
			welcomeSocket = new ServerSocket(3248);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CreateTasks();
		GetTasksList();
	}
	
	private void CreateTasks() {		
		Task tsk = new Task();
		tsk.id = "1";
		tsk.complexity = 0.0f;
		tsk.added = new Date(System.currentTimeMillis());
		tasksList.add(tsk);
		tasksMap.put(tsk.id, "text java");
	}
	
	private String GetTasksList() {
		String list = "";
		for (Task task : tasksList) {
			list.concat(task.toString());			
			list.concat("\n");
		}
		return list;
		
	}
	
	
	private void StartListening() throws IOException {
		String clientSentence = null;		
		while (true) {
			Socket connectionSocket = null;
			BufferedOutputStream outToClient = null;
			BufferedReader inFromClient = null;

			connectionSocket = welcomeSocket.accept();
			outToClient = new BufferedOutputStream(connectionSocket.getOutputStream());
			inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			
			clientSentence = inFromClient.readLine();
			if (clientSentence != null) {
				
				if (clientSentence.equals("1")) { // requestTasksList
					if (outToClient != null) {
						String tasks = GetTasksList();
						DataOutputStream outputString = new DataOutputStream(connectionSocket.getOutputStream());
						if (!tasksMap.isEmpty())
							outputString.writeBytes(serverResponses.tasksAvailable.toString()); // tasks available
						else 
							outputString.writeBytes(serverResponses.tasksUnavailable.toString()); // tasks unavailable
						outputString.writeBytes(tasks);	
						outputString.writeBytes("Finished");
					}
				}
				else if (clientSentence.equals("2")) { // choose task
					
					if (outToClient != null) {						
						
						if (tasksMap.isEmpty()) {
							DataOutputStream outputString = new DataOutputStream(connectionSocket.getOutputStream());
							outputString.writeBytes("No more tasks available.");
						} else {
							String taskID = inFromClient.readLine();
							//String FileName = "text.java";
							String fileInfo = tasksMap.get(taskID);
							
							if (fileInfo != null) {
								File myFile = new File(projectDir + "\\" + fileInfo);
			
								byte[] mybytearray = new byte[(int) myFile.length()];
			
								FileInputStream fis = null;
			
								try {
									fis = new FileInputStream(myFile);
								} catch (FileNotFoundException ex) {
									ex.printStackTrace();
								}
								BufferedInputStream bis = new BufferedInputStream(fis);
			
								try {
									bis.read(mybytearray, 0, mybytearray.length);
									outToClient.write(mybytearray, 0,
											mybytearray.length);
									
									outToClient.flush();
									outToClient.close();
									System.out.println("Finished sending!");
			
									tasksMap.remove(taskID);
									
								} catch (IOException ex) {
									ex.printStackTrace();
								}
							} else {
								//DataOutputStream outputString = new DataOutputStream(connectionSocket.getOutputStream());
								//outputString.writeBytes("Task unavailable.");
							}
						}
					}
				}
				else { // file output
					System.out.println("Result: ");
					System.out.println(clientSentence);
					connectionSocket.close();
					welcomeSocket.close();
				}
			}
		}
	}
	
	public static void main(String args[]) throws IOException {
		TCPServer server = new TCPServer();
		server.StartListening();
	}
}