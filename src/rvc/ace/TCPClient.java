package rvc.ace;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


class TCPClient {

	final static String projectDir = System.getProperty("user.dir");
	private Socket m_clientSocket = null;
	private InputStream m_inputStream = null;
	private BufferedReader m_inFromClient = null;
	private customDataOutputStream m_outputStream = null;
	private boolean m_busy = false;
	private boolean m_tasksAvailable = true;
	private Task m_currentTask = null;

	private String m_IP = null;
	private int m_port = 0;

	public enum clientRequests {
		requestTasksList("1"), chooseTask("2"), fileOutput("3");

		private final String m_request;

		clientRequests(String request) {
			m_request = request;
		}

		@Override
		public String toString() {
			return m_request + "\n";
		}
	};

	class customDataOutputStream extends DataOutputStream {

		public customDataOutputStream(OutputStream out) {
			super(out);
		}

		public void writeBytes(clientRequests request) throws IOException {
			super.writeBytes(request.toString());
		}
	}

	public TCPClient(String ip, int port) {

		try {
			m_clientSocket = new Socket(ip, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		m_IP = ip;
		m_port = port;
	}
	
	private void restartStreams() throws IOException
	{
		m_outputStream = new customDataOutputStream(
				m_clientSocket.getOutputStream());

		m_inputStream = m_clientSocket.getInputStream();
		m_inFromClient = new BufferedReader(new InputStreamReader(
				m_inputStream));
	}

	public void startListening() {
		if(m_clientSocket==null)
			return;
		try {
			restartStreams();

			while (true) {

				if (!m_busy) // Process requests
				{
					getTasksList();
				}
				if (!m_tasksAvailable) {
					m_clientSocket.close();
					System.out.println("closing client: "+m_port);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Task getTaskFromString(String task) {
		Task newTask = new Task();
		int start = 0, finish = 0;
		
		finish = task.indexOf(" ");
		newTask.id = task.substring(start, finish);
		
		start = finish + 1;
		finish = task.indexOf(" ", start);
		newTask.complexity = Float.parseFloat(task.substring(start, finish));
	
		start = finish + 1;
		DateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss zzzz yyyy");
		String strDate = task.substring(start, task.length());
		try {
			newTask.added = formatter.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        
		return newTask;
	}

	private void getTasksList() {

		try {
			m_outputStream.writeBytes(clientRequests.requestTasksList);
			System.out.println("Requesting task list....");
		
		String response;
		List<Task> availableTasks = new ArrayList<Task>();
		if (m_inFromClient != null) {
			response = m_inFromClient.readLine();
			if (response.equals("1")) { // server has tasks

				String task = "";
				task = m_inFromClient.readLine();
				while (!task.equals("Finished")) {
					
					availableTasks.add(getTaskFromString(task));
					task = m_inFromClient.readLine();
				};
				
				Collections.sort(availableTasks, new Comparator<Task>() {
					@Override
					public int compare(Task task1, Task task2) {
						return task1.compareTo(task2);
					}
				});
				m_busy = true;
				m_currentTask = availableTasks.get(0);
				m_tasksAvailable=true;
				System.out.println("Finished receiving task info....");
				m_currentTask.print();
				
				getFile();
			} else {
				m_tasksAvailable = false;
			}
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getFile() {
		byte[] aByte = new byte[4];

		try {
			m_outputStream.writeBytes(clientRequests.chooseTask);
			m_outputStream.writeBytes(m_currentTask.id+"\n");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (m_inputStream != null) {

			try {
				String dataFromServer = m_inFromClient.readLine();
				int start, finish;
				finish = dataFromServer.indexOf(" ");
				String fileName = dataFromServer.substring(0, finish);
				start = finish + 1;
				finish = dataFromServer.indexOf(" ", start);
				String extension = dataFromServer.substring(start, finish);
				String arguments = dataFromServer.substring(finish + 1);
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				fos = new FileOutputStream(projectDir + "\\" + fileName + "."
						+ extension);
				bos = new BufferedOutputStream(fos);
				int count;

				while ((count = m_inputStream.read(aByte)) > 0) {
					bos.write(aByte, 0, count);
				}

				bos.flush();
				bos.close();

				System.out.println("Finished receiving file....");
				executeFile(fileName, extension, arguments);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
	}

	private void executeFile(String filename, String extension, String arguments) {

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
				sendResult(line);
			}
			while ((line = reade2r.readLine()) != null) {
				System.err.println(line);
			}
			p.destroy();

			Files.walk(Paths.get(projectDir)).forEach(
					filePath -> {
						if (Files.isRegularFile(filePath)
								&& filePath.getFileName().toString()
										.contains(filename) 
								&& !(filePath.getParent().toString()
										.contains("Tasks"))) {
							File file = filePath.toFile();
							file.delete();
						}
					});

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void sendResult(String result) {
		try {
			m_clientSocket = new Socket(m_IP, m_port);
			restartStreams();
			m_outputStream.writeBytes(clientRequests.fileOutput);
			m_outputStream.writeBytes(result + '\n');
			m_busy = false;
			System.out.println("Finished sending result....");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String args[]) {
		TCPClient client = new TCPClient("127.0.0.1", 3248);
		client.startListening();

	}
}