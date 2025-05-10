import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
    
public class Account {
	private String password;
	private int balance;

        public Account(String password, int balance) {
            	this.password = password;
            	this.balance = balance;
        }

        // 取款方法增加保存逻辑
        public String getPassword() { return password; }
        public int getBalance() { return balance; }
        public boolean withdraw(int amount) {
            	if (balance >= amount) {
                	balance -= amount;
                	return true;
        	}
            	return false;
        }
}

