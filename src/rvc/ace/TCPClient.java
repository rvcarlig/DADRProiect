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
import java.util.List;

class TCPClient {

	final static String projectDir = System.getProperty("user.dir");
	private Socket m_clientSocket = null;
	private InputStream m_inputStream = null;
	private BufferedReader m_inFromClient = null;
	private customDataOutputStream m_outputStream = null;
	private boolean m_busy = false;
	private boolean m_tasksAvaialble = true;
	private Task m_currentTask = null;

	private String m_IP = null;
	private int m_port = 0;

	public enum clientRequests {
		requestTasksList("1\n"), chooseTask("2\n"), fileOutput("3\n");

		private final String m_request;

		clientRequests(String request) {
			m_request = request;
		}

		@Override
		public String toString() {
			return m_request;
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

	public void startListening() {

		try {
			m_outputStream = new customDataOutputStream(
					m_clientSocket.getOutputStream());

			m_inputStream = m_clientSocket.getInputStream();
			m_inFromClient = new BufferedReader(new InputStreamReader(
					m_inputStream));

			while (true) {
				if (!m_busy) // Process requests
				{
					getTasksList();
				}
				if (!m_tasksAvaialble) {
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			newTask.added = formatter.parse(task.substring(start));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newTask;
	}

	private void getTasksList() throws IOException {
		m_outputStream.writeBytes(clientRequests.requestTasksList);
		String response;
		List<Task> availableTasks = new ArrayList<Task>();
		if (m_inFromClient != null) {
			response = m_inFromClient.readLine();
			if (response.equals("1")) { // server has tasks
				String task = null;
				do {
					task = m_inFromClient.readLine();
					System.out.println(task);
					availableTasks.add(getTaskFromString(task));
				} while (!task.equals("Finished"));
				Collections.sort(availableTasks, new Comparator<Task>() {
					@Override
					public int compare(Task task1, Task task2) {
						return task1.compareTo(task2);
					}
				});
				m_busy = true;
				m_currentTask = availableTasks.get(0);
				getFile();
			} else {
				m_tasksAvaialble = false;
			}
		}
	}

	private void getFile() {
		byte[] aByte = new byte[4];

		try {
			m_outputStream.writeBytes(clientRequests.chooseTask);
			m_outputStream.writeBytes(m_currentTask.id);
		} catch (IOException ex) {
			// Do exception handling
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
				String arguments = dataFromServer.substring(dataFromServer
						.indexOf(" ", finish + 1));
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

				System.out.println("Finished receiving!");
				executeFile(fileName, extension, arguments);
			} catch (IOException ex) {
				// Do exception handling
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
				System.out.println(line);
				// send output
				sendResult(line);
			}
			while ((line = reade2r.readLine()) != null) {
				System.out.println(line);
			}
			p.destroy();

			Files.walk(Paths.get(projectDir)).forEach(
					filePath -> {
						if (Files.isRegularFile(filePath)
								&& filePath.getFileName().toString()
										.contains("sss")) {
							File file = filePath.toFile();
							file.delete();
							System.out.println(filePath.getFileName());
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
			m_outputStream = new customDataOutputStream(
					m_clientSocket.getOutputStream());
			m_outputStream.writeBytes(clientRequests.fileOutput);
			m_outputStream.writeBytes(result + '\n');
			m_clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String args[]) {
		TCPClient client = new TCPClient("127.0.0.1", 3248);
		client.startListening();

	}
}