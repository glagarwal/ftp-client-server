import java.net.*;
import java.io.*;
import java.util.*;

class myftp implements Runnable {
	public static final Boolean DEBUG = false;
	public static final String GET_COMMAND = "get ";
	public static final String PUT_COMMAND = "put ";
	public static final String UNEXPECTED_ERROR = "Unexpected error occured";
	public static final String download_dir = System.getProperty("user.dir");
	public DataInputStream dis;
	public DataOutputStream dos;
	public static Scanner sc = new Scanner(System.in);

	public static Socket s;
	public static Socket terminateSocket;
	public static DataInputStream dis_terminate;
	public static DataOutputStream dos_terminate;
	public static final String TERMINATE_COMMAND = "terminate ";
	public static final String TERMINATE_SUCCESSFUL = "terminated";
	public static final String N_PORT = "nport";
	public static final String T_PORT = "tport";
	public static final String RUN_ON_NEW_THREAD = " &";

	public static Thread t = null;
	private String cmd;

 	public static boolean stop_transfer = false;
	public static boolean isFileDeleted = false;

	public synchronized boolean getIsFileDeleted(){
		return this.isFileDeleted;
	}
	public synchronized void setIsFileDeleted(boolean b){
		this.isFileDeleted = b;
	}
	//-----
	public synchronized void stopFileTransfer(boolean b){
	 	this.stop_transfer = b;
	}
	public synchronized boolean checkStopTransfer(){
		return this.stop_transfer;
	}

	public myftp(){
		try{
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//----------------------Constructor to instantiate myftp child thread (Created to make client multi-threaded)-----------
	public myftp(String command) {
		try{
			this.cmd = command;
			this.dis = new DataInputStream(s.getInputStream());
			this.dos = new DataOutputStream(s.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			if (cmd.contains(GET_COMMAND)){
				if(DEBUG)	System.out.println("I am in thread-get command");
				this.get(cmd.split(" ")[1], s);
			}
			else{
				if(DEBUG)	System.out.println("I am in thread - put command");
				this.put(cmd.split(" ")[1], s);
			}
			System.out.print("\nmyftp> ");
			t=null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//---------------GET files-----------------
	public void get(String fileName, Socket s) {
		try {

			if(DEBUG) System.out.println("In get method"+this.dos.size());
			// String fileName = fname;
			String repFromServer = this.dis.readUTF();
			if(DEBUG) System.out.println("Reply from server is---- "+repFromServer);
			if (repFromServer.compareTo("File Not Found") == 0) {
				System.out.println("File not found on Server ...");
				return;
			} else if (repFromServer.compareTo("found") == 0) {
				System.out.println("Receiving File ...");
				if(t!=null)
					System.out.print("(Terminate ongoing command before anything else!) \nmyftp> ");
				File f = new File(download_dir + "/" + fileName);
				// File f=new File(fileName);
				// if (f.exists()) {
				// 	// String Option;
				// 	System.out.print("File Already Exists. Want to OverWrite (Y/N) ?	");
				// 	String opt = sc.nextLine();
				// 	if (opt.compareTo("N") == 0) {
				// 		this.dos.writeUTF("Cancel");
				// 		return;
				// 	}
				// }
				// dos.writeUTF("Continue");
				FileOutputStream fout = new FileOutputStream(f);
				int ch;
				String temp;
				long lStartTime = System.currentTimeMillis();
				do {
					temp = this.dis.readUTF();
					if (temp.equalsIgnoreCase("delete")){
						if(DEBUG) System.out.println("In deleting");
						fout.close();
						f.delete(); //delete the incompletely transferred file.
						this.setIsFileDeleted(true);
						System.out.print("Reply: "+this.dis.readUTF());
						return;
					}
					//System.out.println("temp is "+temp);
					ch = Integer.parseInt(temp);
					if (ch != -1) {
						fout.write(ch);
					}
				} while (ch != -1);
				fout.close();
				long lEndTime = System.currentTimeMillis();
				long output = lEndTime - lStartTime;
				if(t!=null)
					System.out.print("Reply: "+this.dis.readUTF());
				else
					System.out.println(
						"Elapsed time: " + (output / 1000.0) + "seconds or " + (output / (1000.0 * 60)) + "minutes");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//---------------PUT files-----------------
	public void put(String fileName, Socket s) {
		try {
			// String fileName = command.substring(4);
			File f = new File(fileName);
			// dos.writeUTF("Going ahead");
			String repFromServer = "";//dis.readUTF();

			// if (repFromServer.compareTo("File already exists in Server") == 0) {
			// 	System.out.print("File already exists in Server. Want to OverWrite (Y/N) ? ");
			// 	String opt = sc.nextLine();
			// 	if (opt.compareTo("N") == 0) {
			// 		System.out.println("Your selected option = " + opt);
			// 		dos.writeUTF("N");
			// 		return;
			// 	} else
			// 		dos.writeUTF("Y");
			// } else
			//System.out.println("Rep from Server ---- "+repFromServer);
			FileInputStream fin = new FileInputStream(f);
			int ch;
			do {
				if(checkStopTransfer()){
					if(DEBUG) System.out.println("In deleting");
					fin.close();
					dos.flush();
					this.dos.writeUTF("delete");
					repFromServer = this.dis.readUTF();
					if(repFromServer.equalsIgnoreCase("File transfer terminated")){
						this.setIsFileDeleted(true);
						System.out.print("Reply: "+repFromServer);
					}
					return;
				}
				ch = fin.read();
				dos.writeUTF(String.valueOf(ch));
			} while (ch != -1);
			fin.close();
			if(t!=null)
				System.out.println("Reply: "+this.dis.readUTF());

		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	//---------------main method-----------------
	public static void main(String args[]) {
		try {
			s = new Socket(args[0], Integer.valueOf(args[1]));
		  // Thread t = null;
			myftp client = new myftp();
			String tID = "";
			// dis = new DataInputStream(s.getInputStream()); //get input from the server
			// dos = new DataOutputStream(s.getOutputStream()); //send message to the server

			terminateSocket = new Socket(args[0], Integer.valueOf(args[2]));
			dis_terminate = new DataInputStream(terminateSocket.getInputStream());
			dos_terminate = new DataOutputStream(terminateSocket.getOutputStream());

			String command = "Chat started!";
			String msg = "G";

			while (true) {
				if (DEBUG)	System.out.println("In Main while loop");
				System.out.print("\nmyftp> ");
				command = sc.nextLine();

				if (t==null && command.contains(GET_COMMAND) && command.substring(0, 4).equalsIgnoreCase(GET_COMMAND)) {
					client.dos.writeUTF(command);
					int at_end = command.length() - 2;
					if (command.substring(at_end).equals(RUN_ON_NEW_THREAD)) {
						System.out.println("Starting new thread");
						tID = client.dis.readUTF();
						System.out.println("Command ID: " + tID);
						t = new Thread(new myftp(command));
						// t.setDaemon(true);
						t.start();
						command = null;
					} else
						client.get(command.substring(4), s);
				} else if (t==null && command.contains(PUT_COMMAND) && command.substring(0, 4).equalsIgnoreCase(PUT_COMMAND)) {
					File f = new File(command.split(" ")[1]);
					if (!f.exists()) {
						System.out.println("File Not Found on your Local machine!");
						// client.dos.writeUTF("operation Aborted");
						continue;
					}
					client.dos.writeUTF(command);
					int at_end = command.length() - 2;
					if (command.substring(at_end).equals(RUN_ON_NEW_THREAD)) {
						System.out.println("Starting new thread");
						tID = client.dis.readUTF();
						System.out.println("Command ID: " + tID);
						t = new Thread(new myftp(command));
						t.start();
						command = null;
					} else
						client.put(command.substring(4), s);
				} else if (command.contains(TERMINATE_COMMAND)) {
					if(t == null){
						System.out.println("No ongoing get / put command to terminate!");
						continue;
					}
					client.stopFileTransfer(true);
					dos_terminate.writeUTF(command);
					command = null;
					System.out.print("Okay, please wait! Terminating...");
					do{

					}while(!client.getIsFileDeleted());
					System.out.println("Terminated!");
					client.setIsFileDeleted(false);
					client.stopFileTransfer(false);
					t = null;
					//t.interrupt();
				}else if (t==null){
					if(DEBUG)	System.out.println("Sending to server");
					client.dos.writeUTF(command);
				}

				if (t==null && command != null) {
					if(DEBUG) System.out.println("Reply wait---- ");
					msg = client.dis.readUTF();
					System.out.println("Reply: " + msg);
				}

				if (command != null && command.equalsIgnoreCase("quit")) {
					System.exit(0);
					//break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(UNEXPECTED_ERROR + ": " + e);
		}
	}
}
