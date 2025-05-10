import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionLogger {
    	private static final String LOG_FILE = "withdraw.log";
    	private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    	// 使用同步锁保证线程安全
    	private static final Object lock = new Object();

    	public static void logWithdrawal(String userId, int amount, boolean success) {
        	synchronized (lock) {
            		try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                		String timestamp = LocalDateTime.now().format(formatter);
                		String status = success ? "成功" : "失败（余额不足）";
                		String logEntry = String.format("[%s] %s 取款 %d元 %s%n", 
                    		timestamp, userId, amount, status);
                
                		writer.write(logEntry);
                		writer.flush(); // 立即写入磁盘
            		} catch (IOException e) {
                		System.err.println("日志记录失败: " + e.getMessage());
           		}
        	}
    	}
}