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
import java.io.ObjectInputStream.GetField;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Task{
	public String id;
	public float complexity;
	public Date added;
	
	public Task() {
		
	}
	public String GetAsString()	{
		return (id + " " + String.valueOf(complexity) + " " + added.toString() + "\n");
	}
		
	public int compareTo(Task other)	{
		return (int) (other.complexity-this.complexity);
	}
	
	public void print()
	{
		System.out.println("Task id: "+id);
		System.out.println("Task complexity "+complexity);
		System.out.println("Task date "+added);
	}
}

class TCPServer {

	final static String projectDir = System.getProperty("user.dir");
	private ServerSocket m_welcomeSocket = null;
	private Map<String, String> m_tasksMap = new HashMap<String, String>();
	private List<Task> m_tasksList = new ArrayList<Task>();
	private Socket m_connectionSocket = null;
	private BufferedOutputStream m_outToClient = null;
	private BufferedReader m_inFromClient = null;
	private boolean m_taskAvailable = true;
	
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
			m_welcomeSocket = new ServerSocket(3248);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void CreateTasks() {	
		Task tsk = new Task();
		tsk.id = "1";
		tsk.complexity = 0.0f;
		String date_s = "2015-05-18 16:21:00.0";
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
			tsk.added = dt.parse(date_s);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		m_tasksList.add(tsk);
		m_tasksMap.put(tsk.id, "text java ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f");
		
		tsk = new Task();
		tsk.id = "2";
		tsk.complexity = 0.0f;
		date_s = "2015-06-11 12:41:00.0";
        try {
			tsk.added = dt.parse(date_s);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		m_tasksList.add(tsk);
		m_tasksMap.put(tsk.id, "text java ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f");
		
	}
	
	private String GetTasksList() {
		String list = "";
		for (Task t : m_tasksList) {
			if(m_tasksMap.containsKey(t.id))
				list = list + t.GetAsString() ;			
		}
		return list;	
	}
	
	private void SendTasksList() throws IOException {
		if (m_outToClient != null) {
			String tasks = GetTasksList();
			DataOutputStream outputString = new DataOutputStream(m_connectionSocket.getOutputStream());
			if (!m_tasksMap.isEmpty()) {
				outputString.writeBytes(serverResponses.tasksAvailable.toString()); // tasks available
				outputString.writeBytes(tasks);	
				outputString.writeBytes("Finished\n");
			} else {
				outputString.writeBytes(serverResponses.tasksUnavailable.toString()); // tasks unavailable
				m_taskAvailable = false;
			}
		}
	}
	
	private void SendTask() throws IOException {
		if (m_outToClient != null) {						
			
			if (m_tasksMap.isEmpty()) {
				DataOutputStream outputString = new DataOutputStream(m_connectionSocket.getOutputStream());
				outputString.writeBytes("No more tasks available.");
			} else {
				String taskID = m_inFromClient.readLine();
				String fileInfo = m_tasksMap.get(taskID);
				
				if (fileInfo != null) {
					int start, finish;
					finish = fileInfo.indexOf(" ");
					String fileName = fileInfo.substring(0, finish);
					start = finish + 1;
					finish = fileInfo.indexOf(" ", start);
					String extension = fileInfo.substring(start, finish);
					DataOutputStream outputString = new DataOutputStream(m_connectionSocket.getOutputStream());
					outputString.writeBytes(fileInfo+"\n");
					File myFile = new File(projectDir + "\\Tasks\\" + fileName+"."+extension);

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
						m_outToClient.write(mybytearray, 0,
								mybytearray.length);
						
						m_outToClient.flush();
						m_outToClient.close();
						System.out.println("Finished sending!");
						
						m_connectionSocket = m_welcomeSocket.accept();						
						m_outToClient = new BufferedOutputStream(m_connectionSocket.getOutputStream());
						m_inFromClient = new BufferedReader(new InputStreamReader(m_connectionSocket.getInputStream()));

						m_tasksMap.remove(taskID);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}
	
	private void StartListening() {
		
		String clientSentence = "";	
		try {
		m_connectionSocket = m_welcomeSocket.accept();
		
		m_outToClient = new BufferedOutputStream(m_connectionSocket.getOutputStream());
		m_inFromClient = new BufferedReader(new InputStreamReader(m_connectionSocket.getInputStream()));
		
		while (true) {			
			if(!m_taskAvailable)
			{
				System.out.println("Server Closing!");
				m_connectionSocket.close();
				m_welcomeSocket.close();			
				break;	
			}
			clientSentence = m_inFromClient.readLine();
			if (clientSentence != null) {
				
				if (clientSentence.equals("1")) { // requestTasksList
					SendTasksList();
				}
				else if (clientSentence.equals("2")) { // choose task
					SendTask();					
				}
				
				else if (clientSentence.equals("3")){ // file output
					clientSentence = m_inFromClient.readLine();
					System.out.println("Result: "+clientSentence);
				}
			}
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) throws IOException {
		TCPServer server = new TCPServer();
		
		server.CreateTasks();
		server.StartListening();
	}
}