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
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Task {
	public String id;
	public float complexity;
	public Date added;

	public Task() {

	}

	public String GetAsString() {
		return (id + " " + String.valueOf(complexity) + " " + added.toString() + "\n");
	}

	public int compareTo(Task other) {
		return (int) (other.complexity - this.complexity);
	}

	public void print() {
		System.out.println("Task id: " + id);
		System.out.println("Task complexity " + complexity);
		System.out.println("Task date " + added);
	}

	/**
	 * getTaskFromString method This method receives an available tasks as a
	 * concatenated string. It transforms the data into a Task object and
	 * returns the task.
	 * 
	 * @param task
	 *            a string containing the info about a task
	 * @return returns the task object created
	 */
	static Task getTaskFromString(String task) {
		Task newTask = new Task();
		int start = 0, finish = 0;

		finish = task.indexOf(" ");
		newTask.id = task.substring(start, finish);

		start = finish + 1;
		finish = task.indexOf(" ", start);
		newTask.complexity = Float.parseFloat(task.substring(start, finish));

		start = finish + 1;
		DateFormat formatter = new SimpleDateFormat(
				"EEE MMM d HH:mm:ss zzzz yyyy");
		String strDate = task.substring(start, task.length());
		try {
			newTask.added = formatter.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newTask;
	}
}

/**
 * The TCPServer class implements a server that distributes tasks to clients.
 */
class TCPServer {

	final static String projectDir = System.getProperty("user.dir");
	private ServerSocket m_welcomeSocket = null;
	private Map<String, String> m_tasksMap = new HashMap<String, String>();
	private Map<String, byte[]> m_fileCache = new HashMap<String, byte[]>();
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

	/**
	 * The TCPServer Constructor
	 */
	public TCPServer() {
		try {
			m_welcomeSocket = new ServerSocket(3248);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * CreateTasks method This is the method where the list of tasks is created
	 * and mapped. For each task available there is a java file that represents
	 * the code that will be sent to clients.
	 * 
	 * @return void
	 */
	public void CreateTasks() {

		try {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(projectDir + "\\Tasks\\tasks.txt"),
					Charset.forName("UTF-8")));

			while ((line = br.readLine()) != null) {
				m_tasksList.add(Task.getTaskFromString(line));
			}
			br.close();
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					projectDir + "\\Tasks\\tasksinfo.txt"),
					Charset.forName("UTF-8")));

			while ((line = br.readLine()) != null) {
				int space = line.indexOf(" ");
				m_tasksMap.put(line.substring(0, space),
						line.substring(space + 1));
			}
			br.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * GetTasksList method This method concatenates all the available tasks; on
	 * a line are the three characteristics of a task: id, estimated complexity
	 * and the date when it was added. Is is used when a client requests the
	 * list of tasks to obtain the concatenated string in order to be sent to
	 * the client.
	 * 
	 * @return the concatenated string containing all available tasks
	 */
	private String GetTasksList() {
		String list = "";
		for (Task t : m_tasksList) {
			if (m_tasksMap.containsKey(t.id))
				list = list + t.GetAsString();
		}
		return list;
	}

	/**
	 * SendTasksList method This method is responsible if sending the list of
	 * available tasks to the client that requested it. It verifies if there are
	 * tasks available and sends the appropiate response to the client and if
	 * there are tasks available, it gets the concatenated string by calling the
	 * GetTasksList method and sends it through a DataOutputStream to the
	 * client.
	 * 
	 * @return void
	 */
	private void SendTasksList() throws IOException {
		if (m_outToClient != null) {
			String tasks = GetTasksList();
			DataOutputStream outputString = new DataOutputStream(
					m_connectionSocket.getOutputStream());
			if (!m_tasksMap.isEmpty()) {
				outputString.writeBytes(serverResponses.tasksAvailable
						.toString()); // tasks available
				outputString.writeBytes(tasks);
				outputString.writeBytes("Finished\n");
			} else {
				outputString.writeBytes(serverResponses.tasksUnavailable
						.toString()); // tasks unavailable
				m_taskAvailable = false;
			}
		}
	}

	/**
	 * SendTask method This method is responsible for sending the java file for
	 * the corresponding task that was requested by a client. It first receives
	 * the id of the task requested and then it retrieves and sends to the
	 * client the information about that task (name of the file corresponding to
	 * the task, the extension of the file and the arguments needed to run the
	 * file). Afterwards, the file is sent to the client and it is removed from
	 * the list of available tasks.
	 * 
	 * @return void
	 */
	private void SendTask() throws IOException {
		if (m_outToClient != null) {

			if (m_tasksMap.isEmpty()) {
				DataOutputStream outputString = new DataOutputStream(
						m_connectionSocket.getOutputStream());
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
					DataOutputStream outputString = new DataOutputStream(
							m_connectionSocket.getOutputStream());
					outputString.writeBytes(fileInfo + "\n");
					File myFile = new File(projectDir + "\\Tasks\\" + fileName
							+ "." + extension);

					byte[] mybytearray;
					if (m_fileCache.containsKey(fileName)) {
						mybytearray = m_fileCache.get(fileName);
					} else {
						mybytearray = new byte[(int) myFile.length()];

						FileInputStream fis = null;

						try {
							fis = new FileInputStream(myFile);
						} catch (FileNotFoundException ex) {
							ex.printStackTrace();
						}
						BufferedInputStream bis = new BufferedInputStream(fis);

						bis.read(mybytearray, 0, mybytearray.length);
						m_fileCache.put(fileName, mybytearray);
						bis.close();
						fis.close();
					}
					m_outToClient.write(mybytearray, 0, mybytearray.length);

					m_outToClient.flush();
					m_outToClient.close();
					System.out.println("Finished sending!");

					m_connectionSocket = m_welcomeSocket.accept();
					m_outToClient = new BufferedOutputStream(
							m_connectionSocket.getOutputStream());
					m_inFromClient = new BufferedReader(new InputStreamReader(
							m_connectionSocket.getInputStream()));

					m_tasksMap.remove(taskID);
				}
			}
		}
	}

	/**
	 * StartListening method This method sets up the connection between server
	 * and clients. It receives the requests from clients and calls the
	 * appropiate functions to deal with the requests. The method also receives
	 * the result of the taks sent by a client and prints it in the console.
	 * 
	 * @return void
	 */
	private void StartListening() {

		try {
			m_connectionSocket = m_welcomeSocket.accept();

			m_outToClient = new BufferedOutputStream(
					m_connectionSocket.getOutputStream());
			m_inFromClient = new BufferedReader(new InputStreamReader(
					m_connectionSocket.getInputStream()));

			String clientSentence = "";
			while (true) {
				if (!m_taskAvailable) {
					System.out.println("Server Closing!");
					m_connectionSocket.close();
					m_welcomeSocket.close();
					break;
				}
				clientSentence = m_inFromClient.readLine();
				if (clientSentence != null) {

					if (clientSentence.equals("1")) { // requestTasksList
						SendTasksList();
					} else if (clientSentence.equals("2")) { // choose task
						SendTask();
					} else if (clientSentence.equals("3")) { // file output
						clientSentence = m_inFromClient.readLine();
						System.out.println("Result: " + clientSentence);
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