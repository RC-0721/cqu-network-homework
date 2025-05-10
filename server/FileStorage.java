import java.io.*;
import java.util.HashMap;

public class FileStorage {
    	private static final String FILE_NAME = "accounts.txt";
    
    	public static void printFilePath() {
        	File file = new File(FILE_NAME);
        	System.out.println("数据文件路径: " + file.getAbsolutePath());
    	}

    	// 从文件加载账户数据
    	public static HashMap<String, Account> loadAccounts() {
        	HashMap<String, Account> accounts = new HashMap<>();
        	try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            		String line;
            		while ((line = br.readLine()) != null) {
                		String[] parts = line.split("\\|");
                		if (parts.length == 3) {
                    			String userid = parts[0];
                    			String password = parts[1];
                    			int balance = Integer.parseInt(parts[2]);
                    			accounts.put(userid, new Account(password, balance));
                		}
            		}
        	} catch (IOException e) {
            		System.out.println("数据文件不存在，将创建新文件");
        	}
        	return accounts;
   	}

    	// 保存账户数据到文件
    	public static synchronized void saveAccounts(HashMap<String, Account> accounts) {
        	try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            		for (String userid : accounts.keySet()) {
                		Account acc = accounts.get(userid);
                		pw.println(userid + "|" + acc.getPassword() + "|" + acc.getBalance());
            		}
        	} catch (IOException e) {
            	System.err.println("数据保存失败: " + e.getMessage());
        	}
    	}
}