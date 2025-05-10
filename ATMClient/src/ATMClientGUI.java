import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.net.*;
import java.util.Scanner;


public class ATMClientGUI extends JFrame {

	private static String SERVER_IP = "119.84.246.217";
	private static int SERVER_PORT = 14641;
	private Socket socket;
    	private PrintWriter out;
    	private BufferedReader in;
    	private volatile boolean isConnected = false;
	
    	private CardLayout cardLayout;
    	private JPanel mainPanel;

    	// 界面标识
    	private static final String LOGIN_PANEL = "login";
    	private static final String MENU_PANEL = "menu";
    	private static final String WITHDRAW_PANEL = "withdraw";

    	public ATMClientGUI() {
        	initUI();
    	}

	private boolean connectToServer() {
        	try {socket = new Socket(SERVER_IP, SERVER_PORT);
            	out = new PrintWriter(socket.getOutputStream(), true);
            	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            	isConnected = true;
            	return true;
        	} catch (IOException e) {
            		JOptionPane.showMessageDialog(this, "连接服务器失败", "错误", JOptionPane.ERROR_MESSAGE);
            		return false;
        	}	
    	}

	private void disconnect() {
        	try {if (socket != null) socket.close();
            	if (out != null) out.close();
            	if (in != null) in.close();
            	isConnected = false;
        	} catch (IOException e) {
            		System.err.println("关闭连接失败: " + e.getMessage());
        	}
    	}

    	private void initUI() {
        	setTitle("ATM客户端");
        	setSize(400,300);
        	setResizable(false);  // 禁用窗口缩放
        	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        	setLocationRelativeTo(null);

        	cardLayout = new CardLayout();
        	mainPanel = new JPanel(cardLayout);

        	// 添加不同界面
        	mainPanel.add(createLoginPanel(), LOGIN_PANEL);
        	mainPanel.add(createMenuPanel(), MENU_PANEL);
        	mainPanel.add(createWithdrawPanel(), WITHDRAW_PANEL);

        	add(mainPanel);
    	}
	
	// 登录流程分为两个面板
	private JPanel createLoginPanel() {
    		JPanel panel = new JPanel(new CardLayout());
    
    		// 用户ID输入面板
    		JPanel userIdPanel = new JPanel(new GridLayout(2, 2, 5, 5));
    		JTextField userIdField = new JTextField();
    		JButton nextBtn = new JButton("下一步");
    
    		userIdPanel.add(new JLabel("用户ID:"));
    		userIdPanel.add(userIdField);
    		userIdPanel.add(new JLabel(""));
    		userIdPanel.add(nextBtn);

    		// 密码输入面板
    		JPanel pinPanel = new JPanel(new GridLayout(3, 2, 5, 5));
    		JPasswordField pinField = new JPasswordField();
    		JButton submitBtn = new JButton("提交");
    		JButton backBtn = new JButton("返回");
    
    		pinPanel.add(new JLabel("密码:"));
    		pinPanel.add(pinField);
    		pinPanel.add(backBtn);
    		pinPanel.add(submitBtn);

    		// 添加到主面板
    		panel.add(userIdPanel, "USER_ID");
    		panel.add(pinPanel, "PIN");

    		// 按钮事件
    		nextBtn.addActionListener(e -> {
        		String userId = userIdField.getText();
        		if (validateUserId(userId)) { // 网络验证
            			((CardLayout)panel.getLayout()).show(panel, "PIN");
        		}
    		});

    		backBtn.addActionListener(e -> ((CardLayout)panel.getLayout()).show(panel, "USER_ID"));

		submitBtn.addActionListener(e -> {
        		char[] passwordChars = pinField.getPassword();
        		String pin = new String(passwordChars);
        		validatePin(pin);			
    		});
		
    		return panel;
	}

	private boolean validateUserId(String userId) {
        	try{out.println("HELO " + userId );
        	String response = in.readLine();
        	System.out.println(response);
        	SwingUtilities.invokeLater(() -> {
            		if ("500 AUTH REQUIRE".equals(response)) {
                	// 已自动切换面板
            		} 
			else {
                		JOptionPane.showMessageDialog(this, "用户不存在", "验证失败", JOptionPane.ERROR_MESSAGE);
            		}
        	});
        	return "500 AUTH REQUIRE".equals(response);
		}catch (IOException e) {
        		System.err.println("Connection error: " + e.getMessage());
			return false;
       		}
	}				


	private void validatePin(String pin) {
		new SwingWorker<Boolean, Void>() {
        		@Override
        		protected Boolean doInBackground() throws Exception {
            			try {out.println("PASS " + pin );
                		String response = in.readLine();
                		System.out.println(response);
                		return "525 OK".equals(response);
            			} catch (IOException e) {
                			return false;
            			}
        		}
			@Override
        		protected void done() {
            			try {
 					if (get()) { // 验证成功
                    			SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, MENU_PANEL));
                			} else { // 验证失败
                    				JOptionPane.showMessageDialog(
                        			ATMClientGUI.this,
                        			"密码错误",
                        			"验证失败",
                        			JOptionPane.ERROR_MESSAGE
                    				);
                			}
            			} catch (Exception ex) {
                			JOptionPane.showMessageDialog(
                    			ATMClientGUI.this,
                    			"验证失败: " + ex.getMessage(),
                    			"错误",
                    			JOptionPane.ERROR_MESSAGE
                			);
           			}
        		}
    		}.execute();
	}

	class BackgroundPanel extends JPanel {
		private Image backgroundImage;
	
		public BackgroundPanel(URL imgURL) {
			try {
				
				backgroundImage = ImageIO.read(imgURL);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (backgroundImage != null) {
				g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
			}
		}
	}
	
	// 首先创建一个音效管理类
	class SoundEffect {
	    private Clip clip;
	    private boolean isLoaded;
	    
	    public SoundEffect(URL url) {
	        try {
	        	
//	            File soundFile = new File(url.getPath());
//
//	            if (!soundFile.exists()) {
//	                System.err.println("音效文件不存在: " + url.getPath());
//	                return;
//	            }
	            
	            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
	            AudioFormat format = audioInputStream.getFormat();
	            DataLine.Info info = new DataLine.Info(Clip.class, format);
	            
	            if (!AudioSystem.isLineSupported(info)) {
	                System.err.println("音频格式不支持");
	                return;
	            }
	            
	            clip = (Clip) AudioSystem.getLine(info);
	            clip.open(audioInputStream);
	            isLoaded = true;
	        } catch (Exception e) {
	            System.err.println("加载音效失败: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }
	    
	    public void play() {
	        if (isLoaded && clip != null) {
	            clip.stop();
	            clip.setFramePosition(0);
	            clip.start();
	        }
	    }
	}
	
	private JPanel createMenuPanel() {
		java.net.URL imgURL = ATMClientGUI.class.getResource("/res/2.png");
    	JPanel menuPanel = new BackgroundPanel(imgURL);
    	menuPanel.setLayout(new GridBagLayout()); // 或其他您想要的布局管理器

		JPanel buttonPanel = new JPanel();
    	buttonPanel.setLayout(new GridLayout(0, 1, 0, 10)); // 垂直布局，按钮之间间距10像素
    	buttonPanel.setOpaque(false); // 设置按钮面板透明
		java.net.URL clickSound_1_URL = ATMClientGUI.class.getResource("/res/1.wav");
		java.net.URL clickSound_2_URL = ATMClientGUI.class.getResource("/res/2.wav");
    	SoundEffect clickSound_1 = new SoundEffect(clickSound_1_URL);    // 点击音效
    	SoundEffect clickSound_2 = new SoundEffect(clickSound_2_URL);
    	
    	
    	JButton balanceBtn = new JButton("查询余额");
		JButton withdrawBtn = new JButton("取款");
    	JButton exitBtn = new JButton("退出");
    		

    	buttonPanel.add(balanceBtn);
    	buttonPanel.add(withdrawBtn);
    	buttonPanel.add(exitBtn);
//    	int panelWidth = 170; // 按钮面板宽度
//        int panelHeight = 200; // 按钮面板高度
//    	menuPanel.addComponentListener(new ComponentAdapter() {
//    	    @Override
//    	    public void componentResized(ComponentEvent e) {
//    	        // 获取主面板的实际大小
//    	        int mainWidth = menuPanel.getWidth();
//    	        int mainHeight = menuPanel.getHeight();
//    	        
//    	        // 计算按钮面板的位置
//    	        int x = mainWidth - panelWidth - 50; // 距离右边50像素
//    	        int y = (mainHeight - panelHeight) / 2; // 垂直居中
//    	        
//    	        buttonPanel.setBounds(x, y, panelWidth, panelHeight);
//    	    }
//    	});
        
    	menuPanel.add(buttonPanel);	

		balanceBtn.addActionListener(e ->{ 
			String balance = balance();
			clickSound_1.play();
			new Thread(() -> {
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(this, "您的余额为："+balance, "提示", JOptionPane.INFORMATION_MESSAGE);		
				});	
			}).start();
		});

    	withdrawBtn.addActionListener(e -> {cardLayout.show(mainPanel, WITHDRAW_PANEL);
		clickSound_2.play();
    	});
    	exitBtn.addActionListener(e -> System.exit(0));

    	return menuPanel;
	}

	

	private String balance() {
		try{
			out.println("BALA");
						String response = in.readLine();
						System.out.println(response);
                        String balance = response.replace("AMNT:", "");
			return balance;
		}catch (IOException e) {
        		System.err.println("Connection error: " + e.getMessage());
			return "";
       		}
	}

	private JPanel createWithdrawPanel() {
    		JPanel panel = new JPanel();

    		JPanel inputPanel = new JPanel();
    		JTextField amountField = new JTextField(10);
    		JButton confirmBtn = new JButton("确认取款");
    		JButton backBtn = new JButton("返回菜单");
    		java.net.URL imgURL = ATMClientGUI.class.getResource("/res/1.jpg");
    		ImageIcon img = new ImageIcon(imgURL);
    		JLabel titleLabel = new JLabel(img);
    		//titleLabel.setBounds(10,10,300,300);
		

    		inputPanel.add(new JLabel("金额:"));
    		inputPanel.add(amountField);
    		inputPanel.add(confirmBtn);

    		panel.add(titleLabel, BorderLayout.NORTH);
    		panel.add(inputPanel, BorderLayout.CENTER);
    		panel.add(backBtn, BorderLayout.SOUTH);

    		confirmBtn.addActionListener(e -> {
        		try {
            			int amount = Integer.parseInt(amountField.getText());
            			// 调用取款逻辑
            			new Thread(() -> {
                			boolean success = withdraw(amount);
                			String balance = balance();
                			SwingUtilities.invokeLater(() -> {
                    				if (success) {
                        				JOptionPane.showMessageDialog(this, "取款成功，当前余额"+balance, "提示", JOptionPane.INFORMATION_MESSAGE);
                    				} 
						else {
                        				JOptionPane.showMessageDialog(this, "余额不足", "错误", JOptionPane.ERROR_MESSAGE);
                    				}
                			});
            			}).start();
        		} catch (NumberFormatException ex) {
            			JOptionPane.showMessageDialog(this, "请输入有效数字", "输入错误", JOptionPane.WARNING_MESSAGE);
        		}
    		});

    		backBtn.addActionListener(e -> cardLayout.show(mainPanel, MENU_PANEL));

    		return panel;
	}

	
	
	private boolean withdraw(int amount) {
		try{out.println("WDRA " + amount);
                String withdrawResponse = in.readLine();
                System.out.println(withdrawResponse);
                System.out.println(withdrawResponse.contains("525") ? "Withdrawal successful" : "Withdrawal failed");
		return true;
		}catch (IOException e) {
        		System.err.println("Connection error: " + e.getMessage());
			return false;
       		}
	}


	public static void main(String[] args) {
		
    		SwingUtilities.invokeLater(() -> {
        		ATMClientGUI gui = new ATMClientGUI();
			gui.connectToServer();
        		gui.setVisible(true);
    		});
	}
}