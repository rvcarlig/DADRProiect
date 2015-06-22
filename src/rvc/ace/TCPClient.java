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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The TCPClient class implements a client that receives tasks from a server. It
 * compiles and runs the tasks and send the results back to the server that sent
 * them.
 */
class TCPClient {

	final static String projectDir = System.getProperty("user.dir");
	private Socket m_clientSocket = null;
	private InputStream m_inputStream = null;
	private BufferedReader m_inFromServer = null;
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

	/**
	 * restartStreams method This method restarts the streams for the
	 * client-server communication.
	 * 
	 * @return void
	 */
	private void restartStreams() throws IOException {
		m_outputStream = new customDataOutputStream(
				m_clientSocket.getOutputStream());

		m_inputStream = m_clientSocket.getInputStream();
		m_inFromServer = new BufferedReader(
				new InputStreamReader(m_inputStream));
	}

	/**
	 * startListening method This method communicates with the server. When the
	 * client is not busy, it requests makes requests while there are still
	 * tasks available on the server. Requests are made by calling the
	 * getTasksList method that receives the list of tasks available, chooses a
	 * task, runs the code and returns the result. When there are no more tasks
	 * available, the method returns and it means that all the tasks were
	 * processed and the results were sent to the server.
	 * 
	 * @return void
	 */
	public void startListening() {
		if (m_clientSocket == null)
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
					System.out.println("closing client: " + m_port);
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * getTasksList method This method collects the list of available tasks from
	 * server. It also sorts the tasks and chooses the apropiate task to be
	 * requested from the server by calling the getFile method. The client is
	 * now set to ‚busy’ until the task is done.
	 * 
	 * @return void
	 */
	private void getTasksList() {

		try {
			m_outputStream.writeBytes(clientRequests.requestTasksList);
			System.out.println("Requesting task list....");

			String response;
			List<Task> availableTasks = new ArrayList<Task>();
			if (m_inFromServer != null) {
				response = m_inFromServer.readLine();
				if (response.equals("1")) { // server has tasks

					String task = "";
					task = m_inFromServer.readLine();
					while (!task.equals("Finished")) {

						availableTasks.add(Task.getTaskFromString(task));
						task = m_inFromServer.readLine();
					}
					;

					Collections.sort(availableTasks, new Comparator<Task>() {
						@Override
						public int compare(Task task1, Task task2) {
							return task1.compareTo(task2);
						}
					});
					m_busy = true;
					m_currentTask = availableTasks.get(0);
					m_tasksAvailable = true;
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

	/**
	 * getFile method This method requests from server the chosen task to be
	 * resolved. Then, it receives the info about the task and the file to run.
	 * After it finishes receiving, it calls the executeFile method where the
	 * code is executed and the result is received.
	 * 
	 * @return void
	 */
	private void getFile() {
		byte[] aByte = new byte[4];

		try {
			m_outputStream.writeBytes(clientRequests.chooseTask);
			m_outputStream.writeBytes(m_currentTask.id + "\n");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (m_inputStream != null) {

			try {
				String dataFromServer = m_inFromServer.readLine();
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

	/**
	 * executeFile method This method compiles and runs the file corresponding
	 * to the current task. After it receives the result, it calls the
	 * sendResult method which sends the result to the server. It also deletes
	 * the file received for the current task, since the result was received and
	 * the file is no longer needed.
	 * 
	 * @param fileName
	 *            a string containing the file name corresponding to the current
	 *            task
	 * @param extension
	 *            a string containing the extension of the file
	 * @param arguments
	 *            a string containing the arguments
	 * @return void
	 */
	private void executeFile(String filename, String extension, String arguments) {
		boolean error = false;
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
				error = true;
			}
			p.destroy();
			
			if(!error)
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

	/**
	 * sendResult method This method receives the result of the current task and
	 * sends it to the server from which it was requested.
	 * 
	 * @param result
	 *            a string containing the result to be sent to the server
	 * @return void
	 */
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