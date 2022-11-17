package ServerFile;
import java.io.*;
import java.sql.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JFrame;

import javax.swing.JOptionPane;

import ServerGUI.ServerGUI;
//ClientHandler class

class ClientHandler extends Thread {
	final DataInputStream dis;
	final DataOutputStream dos;
	final Socket s;
	static String userNameVal;
	static String passVal;
	static String dateInVal;
	String res;
	String userArray;
	String passArray;
	Statement st;
	Connection con;
	
	// Constructor
	public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, String res) {
		this.s = s;
		this.dis = dis;
		this.dos = dos;
		this.res = res;
	}
	
	@Override
	public void run() {

	/*
		// PUBLIC IP VERIFICATION
		// Find public IP address
		String systemipaddress = "";
		try {
			URL url_name = new URL("http://checkip.amazonaws.com");

			BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));

			// reads system IPAddress
			systemipaddress = sc.readLine().trim();
		}
		catch (Exception e) {
			systemipaddress = "Cannot Execute Properly";
		}
	*/
	
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	    System.out.println("System IP Address : " + (localhost.getHostAddress()).trim());
		
	    String systemipaddress =  (localhost.getHostAddress()).trim();
	
		int cCnt = 0;
		String res = "";
		for(int i=0;i<systemipaddress.length();i++)
		{
			if(systemipaddress.charAt(i) == '.')
				cCnt++;
			if(cCnt == 3)
				break;
			res += systemipaddress.charAt(i);
		}

	    systemipaddress = res;
	    
	//
	    
		String iprcv = "";
		try {
			iprcv = dis.readUTF(); // recieve data from client
		} catch (Exception e) {
			System.out.println(e);
		}

		if (systemipaddress.equals(iprcv))
			System.out.println("matched");
		else {
			JOptionPane.showMessageDialog(null, "Unable to connect! (Try login desired wifi)");
			return;
		}	
		
		//Message testing. Getting it
		
		userArray = "";
		passArray = "";
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connectAttend();
			String sql = "Select UserName from login";
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next()) {
				userArray += rs.getString(1) + " ";
			}
			
			sql = "Select Password from login";
			rs = st.executeQuery(sql);
			
			while(rs.next()) {
				passArray += rs.getString(1) + " ";
			}
			
			con.close();
		} catch (Exception e) {
			System.out.println(e);
		}

		String strUser[] = userArray.split(" ");
		String strPass[] = passArray.split(" ");		
		
		userNameVal = "";
		passVal = "";
		dateInVal = "";
		try {
			userNameVal = dis.readUTF();
			passVal = dis.readUTF();
			dateInVal = dis.readUTF();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String flagMatch = "false";
		String cnt = "";
		boolean val = false;
		for(int i=0;i<strUser.length;i++)
		{
			if(strUser[i].equals(userNameVal))
			{
				if(strPass[i].equals(passVal))
				{
//					callFunc();

					try {
						val = checkedAlready(userNameVal);
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					flagMatch = "true";
					try {
						if(!val)
							cnt = incrCnt(strUser[i]);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}
		
		try {
			dos.writeUTF(flagMatch);
			dos.writeUTF(cnt);
			//write Cnt
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		
		if(flagMatch.equals("true"))
		{
//			callFunc();
			try {
//				boolean val = checkedAlready(userNameVal);
				dos.writeUTF(val+"");
//				if(val)
//				{
//IMPORTANT   status:PENDING			//Nov 17th. MOVE THIS TO CLIENT SIDE PAGE. WARNING
//					JOptionPane.showMessageDialog(null, "Already entered");
//					return;
//				}				
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			try {
				Class.forName("com.mysql.jdbc.Driver");
				connectAttend();
				String sql = "INSERT INTO `attendData`(`UserName`, `EntryTime`) VALUES ('"+userNameVal.toString()+"','"+dateInVal.toString()+"')";
				st.executeUpdate(sql);
				res += "User Name : "+ userNameVal + "Time : "+ dateInVal + "\n";
				con.close();
				ServerGUI.table_load();
			}
			catch(Exception e)
			{
				System.out.println(e);
			}
		}
		
		try {
			// closing resources
			this.dis.close();
			this.dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void callFunc() {
		// TODO Auto-generated method stub
		ServerGUI.table_load();
	}

	public void connectAttend() throws SQLException {
		con = DriverManager.getConnection("jdbc:mysql://localhost:3306/wifidb","root","");
		st = con.createStatement();
	}
	
	private String incrCnt(String userName) throws SQLException {
		connectAttend();
		Statement st = con.createStatement();
		String sql = "UPDATE presentCnt SET AttendCnt = AttendCnt+1 WHERE UserName='"+userName+"'";
		st.executeUpdate(sql);
		sql = "SELECT AttendCnt FROM presentCnt WHERE UserName='"+userName+"'";
		ResultSet rs = st.executeQuery(sql);
		int cnt = 0;
		while(rs.next()) {
			cnt = rs.getInt(1);
		}
		
		String resCnt = cnt + "";
		
		return resCnt;
	}
	
	private boolean checkedAlready(String userName) throws SQLException {
		connectAttend();
		Statement st = con.createStatement();
		String sql = "Select UserName, EntryTime from attendData";
		ResultSet rs = st.executeQuery(sql);
		String uName = new String();
		String entryT = new String();
		while(rs.next()) {
			uName = rs.getString(1);
			entryT = rs.getString(2);		
			if(uName.equals(userName) && dateCheckEqual(dateInVal, entryT)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean dateCheckEqual(String d1, String d2) {
		String[] dt1 = d1.split(" "); 
		String[] dt2 = d2.split(" ");
		if(dt1[0].equals(dt2[0]))
			return true;
		return false;
	}
}


public class ServerFile extends JFrame {

//	private static String resStr;
//	private static JLabel resLabel;
		
	public ServerFile() {
//		System.out.println("Constructor called!!");
	}		

	
	public static void main(String[] args) throws IOException {
				
		// server is listening on port 5056
		ServerSocket ss = new ServerSocket(5056);
		
		// running infinite loop for getting
		// client request
		while (true) {
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();
				System.out.println("A new client is connected : " + s);
				
				// obtaining input and out streams
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				System.out.println("Assigning new thread for this client");				
				
				// create a new thread object
				
				ClientHandler t = new ClientHandler(s, dis, dos, "");
				
				// Invoking the start() method
				t.start();
				t.join();
				s.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}