//ATMServer.java
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ATMServer {
	private static final int PORT = 2525;
	private static HashMap<String, Account> accounts = FileStorage.loadAccounts();
	private static final String LOG_FILE = "atm_server.log";
	private static final String MSG_FILE = "MSG.log";

	// 解析 Proxy Protocol 头部
	private static String parseProxyProtocol(Socket socket) throws IOException {
		InputStream in = socket.getInputStream();
		byte[] header = new byte[108]; // Proxy Protocol v1 最大头部长度
		int bytesRead = in.read(header);
		
		if (bytesRead > 0) {
			String headerStr = new String(header, 0, bytesRead);
			// 检查 PROXY protocol v1 头部
			if (headerStr.startsWith("PROXY")) {
				String[] parts = headerStr.trim().split(" ");
				if (parts.length >= 6) {
					return parts[2]; // 客户端 IP 地址
				}
			}
			// 检查 PROXY protocol v2 头部
			else if (header[0] == 0x0D && header[1] == 0x0A && header[2] == 0x0D && header[3] == 0x0A &&
					 header[4] == 0x00 && header[5] == 0x0D && header[6] == 0x0A && header[7] == 0x51 &&
					 header[8] == 0x55 && header[9] == 0x49 && header[10] == 0x54 && header[11] == 0x0A) {
				// 解析 v2 头部
				int version = header[12] >> 4;
				int command = header[12] & 0xF;
				int family = header[13];
				int len = ((header[14] & 0xFF) << 8) | (header[15] & 0xFF);
				
				if (version == 2 && command == 1) {
					if (family == 0x11) { // IPv4
						return String.format("%d.%d.%d.%d",
							header[16] & 0xFF, header[17] & 0xFF,
							header[18] & 0xFF, header[19] & 0xFF);
					}
				}
			}
		}
		return socket.getInetAddress().getHostAddress();
	}

	// 日志记录方法
	private static void logOperation(String clientIP, String userId, String operation, String result) {
		LocalDateTime now = LocalDateTime.now();
		String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String logMessage = String.format("[%s] IP: %s, User: %s, Operation: %s, Result: %s%n",
				timestamp, clientIP, userId, operation, result);
		
		try (FileWriter fw = new FileWriter(LOG_FILE, true);
			 BufferedWriter bw = new BufferedWriter(fw)) {
			bw.write(logMessage);
			System.out.print(logMessage); // 同时在控制台显示
		} catch (IOException e) {
			System.err.println("Error writing to log file: " + e.getMessage());
		}
	}

	private static void logOperation(String clientIP) {
		LocalDateTime now = LocalDateTime.now();
		String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String logMessage = String.format("[%s] New connection from IP: %s%n",
				timestamp, clientIP);
		
		try (FileWriter fw = new FileWriter(LOG_FILE, true);
			 BufferedWriter bw = new BufferedWriter(fw)) {
			bw.write(logMessage);
			System.out.print(logMessage); // 同时在控制台显示
		} catch (IOException e) {
			System.err.println("Error writing to log file: " + e.getMessage());
		}
	}

	private static void logOperation_msg(String msg, String clientIP) {
		LocalDateTime now = LocalDateTime.now();
		String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String logMessage = String.format("[%s] IP: %s MSG: %s%n", timestamp, clientIP, msg);
		
		try (FileWriter fw = new FileWriter(MSG_FILE, true);
			 BufferedWriter bw = new BufferedWriter(fw)) {
			bw.write(logMessage);
		} catch (IOException e) {
			System.err.println("Error writing to log file: " + e.getMessage());
		}
	}

	public static void main(String[] args) {
		ExecutorService pool = Executors.newCachedThreadPool();
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Server started on port " + PORT);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				pool.execute(new ClientHandler(clientSocket));
			}
		} catch (IOException e) {
			System.err.println("Server error: " + e.getMessage());
		}
	}

	private static class ClientHandler implements Runnable {
		private final Socket clientSocket;
		private String currentUser;
		private final String clientIP;

		public ClientHandler(Socket socket) {
			this.clientSocket = socket;
			String ip = null;
			try {
				ip = parseProxyProtocol(socket);
			} catch (IOException e) {
				System.err.println("Error parsing PROXY protocol: " + e.getMessage());
				ip = socket.getInetAddress().getHostAddress();
			}
			this.clientIP = ip;
			logOperation(this.clientIP);
		}

		@Override
		public void run() {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					logOperation_msg(inputLine,clientIP);
					String[] tokens = inputLine.split(" ");
					switch (tokens[0]) {
						case "HELO":
							currentUser = tokens[1];
							if (accounts.containsKey(currentUser)) {
								out.println("500 AUTH REQUIRE");
								logOperation(clientIP, currentUser, "LOGIN_ATTEMPT", "AUTH_REQUIRED");
							} else {
								out.println("401 ERROR");
								logOperation(clientIP, currentUser, "LOGIN_ATTEMPT", "USER_NOT_FOUND");
							}
							break;
						case "PASS":
							if (accounts.get(currentUser).getPassword().equals(tokens[1])) {
								out.println("525 OK");
								logOperation(clientIP, currentUser, "LOGIN", "SUCCESS");
							} else {
								out.println("401 ERROR");
								logOperation(clientIP, currentUser, "LOGIN", "FAILED");
							}
							break;
						case "BALA":
							double balance = accounts.get(currentUser).getBalance();
							out.println("AMNT:" + balance);
							logOperation(clientIP, currentUser, "BALANCE_CHECK", 
									"SUCCESS (Balance: " + balance + ")");
							break;
						case "WDRA":
							int amount = Integer.parseInt(tokens[1]);
							boolean success = accounts.get(currentUser).withdraw(amount);

							if (success) {
								out.println("525 OK");
								FileStorage.saveAccounts(accounts);
								logOperation(clientIP, currentUser, "WITHDRAWAL", 
										"SUCCESS (Amount: " + amount + ")");
							} else {
								out.println("401 ERROR");
								logOperation(clientIP, currentUser, "WITHDRAWAL", 
										"FAILED (Amount: " + amount + ")");
							}
							break;
						case "BYE":
							logOperation(clientIP, currentUser, "LOGOUT", "SUCCESS");
							return;
					}
				}
			} catch (IOException e) {
				System.err.println("Client handling error: " + e.getMessage());
				logOperation(clientIP, currentUser, "ERROR", e.getMessage());
			}
		}
	}
}