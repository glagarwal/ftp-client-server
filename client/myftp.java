import java.net.*;
import java.io.*;
import java.util.*;

class myftp extends Thread{

	public static final String GET_COMMAND = "get ";
	public static final String PUT_COMMAND = "put ";
	public static final String UNEXPECTED_ERROR = "Unexpected error occured";
	public static final String download_dir = System.getProperty("user.dir");
	public static DataInputStream dis;
	public static DataOutputStream dos;
	public static Scanner sc=new Scanner(System.in);
	public static Socket s;
	// This section is for feature of termination port and multithreading.
	// see here for details: https://github.com/glagarwal/ftp-client-server/issues/13.
	public static final String TERMINATE_COMMAND = "terminate ";
	public static final String TERMINATE_SUCCESSFUL = "terminated";
	public static final String N_PORT = "nport";
	public static final String T_PORT = "tport";
	public String portType;
	public int portNumber;
	public int terminatePortNumber;
	public String machineName = "";
	// isTerminate is used to determine wether client has already connected to tport or not.
	public boolean isTerminate = false;
	public static Socket terminateSocket;

	//----------------------Constructor to instantiate myftp main thread (Created to make client multi-threaded)-----------
	public myftp(String machineName, int portNumber, int terminatePortNumber, String portType){
		try{
			this.portType = portType;
			this.portNumber = portNumber;
			this.machineName = machineName;
			this.terminatePortNumber = terminatePortNumber;

			this.s = new Socket(machineName,portNumber);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
//---------------GET files-----------------
	public static void get(String command, Socket s){
		try{
			String fileName = command.substring(4);
			String repFromServer=dis.readUTF();

			if(repFromServer.compareTo("File Not Found")==0)
			{
					System.out.println("File not found on Server ...");
					return;
			}
			else if(repFromServer.compareTo("found")==0)
			{
					System.out.println("Receiving File ...");
					File f=new File(download_dir+"/"+fileName);
					// File f=new File(fileName);
					if(f.exists())
					{
							// String Option;
							System.out.print("File Already Exists. Want to OverWrite (Y/N) ?	");
							String opt=sc.nextLine();
							if(opt.compareTo("N")==0)
							{
									dos.writeUTF("Cancel");
									return;
							}
					}
					dos.writeUTF("Continue");
					FileOutputStream fout=new FileOutputStream(f);
					int ch;
					String temp;
					long lStartTime = System.currentTimeMillis();
					do
					{
							temp=dis.readUTF();
							ch=Integer.parseInt(temp);
							if(ch!=-1)
							{
									fout.write(ch);
							}
					}while(ch!=-1);
					fout.close();
					long lEndTime = System.currentTimeMillis();
					long output = lEndTime - lStartTime;
					System.out.println("Elapsed time: " + (output/1000.0)+"seconds or "+ (output/(1000.0*60))+"minutes");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
	}

//---------------PUT files-----------------
	public static void put(String command, Socket s){
		try{
				String fileName = command.substring(4);
        File f=new File(fileName);
				dos.writeUTF("Going ahead");
				String repFromServer = dis.readUTF();

					if(repFromServer.compareTo("File already exists in Server")==0){
						System.out.print("File already exists in Server. Want to OverWrite (Y/N) ? ");
						String opt=sc.nextLine();
						if(opt.compareTo("N")==0)
						{
							System.out.println("Your selected option = "+opt);
								dos.writeUTF("N");
								return;
						}
						else
							dos.writeUTF("Y");
					}
					else
						System.out.println(repFromServer);
          FileInputStream fin=new FileInputStream(f);
          int ch;
          do
          {
              ch=fin.read();
              dos.writeUTF(String.valueOf(ch));
          }
          while(ch!=-1);
          fin.close();

		}catch(Exception e){
			e.printStackTrace();

		}
	}
	//--------------------------------run() method is overridden here to execute client tasks------------------------------
	public void run(){
		try{
			this.dis=new DataInputStream(this.s.getInputStream());	//get input from the server
			this.dos=new DataOutputStream(this.s.getOutputStream());	//send message to the server
			String command = "Chat started!";
			String msg = "G";
			while(true){
				System.out.println("In while loop");
				System.out.print("mytftp> "+ this.portType+" >");
				command = sc.nextLine();
				if(!command.contains(TERMINATE_COMMAND)){
					// Do normal main thread stuff
					if(command.equalsIgnoreCase("quit")){
						//quit command on main thread should also quit the tport connection
						dos.writeUTF(command);
						// we send quit command to tport server only if it exists in the first place
						if(this.isTerminate){
							DataOutputStream t_dos=new DataOutputStream(terminateSocket.getOutputStream());
							t_dos.writeUTF(command);
						}
					}else{
						// Send normal commands
						dos.writeUTF(command);
					}
					if(command.contains(GET_COMMAND) && command.substring(0,4).equalsIgnoreCase(GET_COMMAND)){
						get(command, s);
					}
					else if(command.contains(PUT_COMMAND) && command.substring(0,4).equalsIgnoreCase(PUT_COMMAND)){
						File f=new File(command.substring(4));
						if(!f.exists())
						{
								System.out.println("File Not Found on your Local machine!");
								dos.writeUTF("operation Aborted");
								continue;
						}
						put(command, s);
					}
					msg = dis.readUTF();
					System.out.println("Reply: " +msg);

					if(command.equalsIgnoreCase("quit")){
						break;
					}
				}else{
					// If terminate command is called first time, create a connection to tport server
					if(!this.isTerminate){
						this.terminateSocket = new Socket(this.machineName, this.terminatePortNumber);
						this.isTerminate = true;
					}
					System.out.println("command is terminate");
					DataInputStream t_dis=new DataInputStream(terminateSocket.getInputStream());	//get input from the server
					DataOutputStream t_dos=new DataOutputStream(terminateSocket.getOutputStream());	//send message to the server
					t_dos.writeUTF(command);
					String msg_t = t_dis.readUTF();
					System.out.println("connected to tport "+this.terminatePortNumber);
					System.out.println("returned msg is "+msg_t);
					//reading if correct message from server is recieved
					if(msg_t.equalsIgnoreCase(TERMINATE_SUCCESSFUL)){
						System.out.println("Killing the thread");
					 }
				}
			}
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
	//-------------------run() method ends-------------------------------------------
	//---------------main method-----------------
	public static void main(String args[]){
		try{
		  // This method is modified so that when this class is invoked, it will spawn off
			// main client thread.
			myftp mainThread = new myftp(args[0], Integer.valueOf(args[1]), Integer.valueOf(args[2]), N_PORT);
			mainThread.start();
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
