import java.net.*;
import java.io.*;
import java.util.*;

class tport_get_put extends Thread
{
    public DataInputStream dis;
    public DataOutputStream dos;
    public ServerSocket tportServer;
    // Constructor
    public tport_get_put(ServerSocket nportServer, ServerSocket tportServer)
    {
				this.tportServer = tportServer;
        this.dis = new DataInputStream(nportServer.getInputStream());
        this.dos = new DataOutputStream(nportServer.getOutputStream());
    }

		class tport extends Thread{
			ServerSocket tportServer;
			public tport(ServerSocket tportServer) {
				this.tportServer = tportServer;
			}
			public run(){
				Socket tportClient = this.tportServer.accept();
			}
		}

		@Override
    public void run(){
			tport tportThread = new tport(this.tportServer);
			tportThread.start();
			if(cmd.contains("GET"))
					this.sendFile(dos, dis, s, cmd.substring(4));
			else if(cmd.contains("PUT"))
					this.receiveFile(dos, dis, s, cmd.substring(4));
		}

		//------------------sendFile in correspondence to the get command from client-------------------
		public void sendFile(DataOutputStream dos, DataInputStream dis, Socket s, String fileName){
				try{
		        File f=new File(current_dir+"/"+fileName);
		        if(!f.exists())
		        {
		            dos.writeUTF("File Not Found");
								dos.writeUTF("operation Aborted");
		            return;
		        }
		        else
		        {
		            dos.writeUTF("found");
								if(dis.readUTF().compareTo("Cancel")==0){
									dos.writeUTF("Opertion aborted");
									return;
								}
		            FileInputStream fin=new FileInputStream(f);
		            int ch;
								long transfered = 0;
								long file_size = 0;
		            do
		            {
		                ch=fin.read();
										transfered++;
										file_size++;
										if(transfered>=1000){
											if(dis.readUTF().compareTo("terminate")==0){
												dos.writeUTF("File transfer interrupted");
												return;
											}
											transfered = 0;
										}
		                dos.writeUTF(String.valueOf(ch));
		            }
		            while(ch!=-1);
		            fin.close();
		            dos.writeUTF("File Received Successfully. File size: "+file_size);
		        }
				}catch(Exception e){
					e.printStackTrace();
				}
			}//------------------end of sendFile()-------------------

		//------------------receiveFile in correspondence to the put command from client-------------------
		 public void receiveFile(DataOutputStream dos, DataInputStream dis, Socket s, String fileName){
			 try{
				 File f=new File(current_dir+"/"+fileName);

				 if(dis.readUTF().compareTo("operation Aborted")==0){
					 // dos.writeUTF("operation Aborted");
					 return;
				 }

				 	if(f.exists()){
					 	dos.writeUTF("File already exists in Server");
						String opt = dis.readUTF();
						if(opt.compareTo("N")==0){
							System.out.println("Not overwritten");
							dos.writeUTF("Aborted operation");
							return;
						}
				 	}
					else
						dos.writeUTF("Sending...");

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
					dos.writeUTF("Transfer complete\nElapsed time: " + (output/1000.0)+"seconds or "+ (output/(1000.0*60))+"minutes");

			 }catch(Exception e){
				 e.printStackTrace();
			 }
		 }//------------------end of receiveFile()-------------------
}

class myftpserver extends Thread{
	public static final String root_dir = System.getProperty("user.dir");
	public static String current_dir = root_dir;

	public static final String PWD_COMMAND = "pwd";

	public static final String MKDIR_COMMAND = "mkdir ";
	public static final String MKDIR_SUCCESS_MESSAGE = "directory created";

	public static final String CD_FAILURE_MESSAGE = "directory does not exist";
	public static final String CD_COMMAND = "cd ";
	public static final String CD_SUCCESS_MESSAGE = "directory changed to ";
	public static final String CD_BACK_COMMAND = "..";
	public static final String CD_ROOT_MESSAGE = "You can't go beyond root directory";

	public static final String LS_COMMAND = "ls";
	public static final String LS_NO_SUBDIR = "No files or subdirectories";

	public static final String DELETE_COMMAND = "delete ";
	public static final String FILE_NOT_PRESENT = "File does not exist";
	public static final String FILE_DELETED = "File deleted";

	public static final String GET_COMMAND = "get ";
	public static final String PUT_COMMAND = "put ";

	public static final String QUIT_COMMAND = "quit";
	public static final String QUIT_MESSAGE = "FTP Connection closed";

	public static final String INVALID_CMD_MESSAGE = "Invalid command.";
	public static final String UNEXPECTED_ERROR = "Unexpected error occured";
	public static final String WAITING_MSG = "Waiting for Connection...";

	//This section is for feature of termination port and multithreading.
	//see here for details: https://github.com/glagarwal/ftp-client-server/issues/13.
	public static final String T_PORT_CALL = " &";
	public static final String TERMINATE_COMMAND = "terminate ";
	public static final String N_PORT = "nport";
	public static final String T_PORT = "tport";
	public static final String TERMINATE_SUCCESSFUL = "terminated";

	//This flag is used to indicate that tport is now connected and terminate command has been called once
	public boolean isTerminated = false;

	//----------------------Constructor to instantiate myftpserver thread (Created to make server multi-threaded)-----------
	myftpserver(int portNumber, String portType){
		try{
			this.portType = portType;
			this.portNumber = portNumber;
			this.server=new ServerSocket(portNumber);
			System.out.println("Server started for "+this.portNumber+" "+this.portType);
		}catch(Exception e){
			e.printStackTrace();
		}
	}//------------------end of Constructor---------------------
	//------------------printWorkingDirectory in correspondence to the pwd command from client-------------------
	public static void printWorkingDirectory(DataOutputStream dos) throws Exception{
		try{
				dos.writeUTF(current_dir);
		}catch(Exception e){
			dos.writeUTF(UNEXPECTED_ERROR);
		}
	}//------------------end of printWorkingDirectory()-------------------

	//------------------makeDirectory in correspondence to the mkdir command from client-------------------
	public static void makeDirectory(DataOutputStream dos, String dir_name) throws Exception{
		try{
			File dir = new File(current_dir.concat("/").concat(dir_name.substring(6)));
			dir.mkdirs();
			dos.writeUTF(MKDIR_SUCCESS_MESSAGE);
		}catch(Exception e){
			dos.writeUTF(UNEXPECTED_ERROR);
		}
	}//------------------end of makeDirectory()-------------------

	//------------------changeDirectory in correspondence to the cd command from client-------------------
	public static void changeDirectory(DataOutputStream dos, String dir) throws Exception{
		try{
			if(!dir.equalsIgnoreCase(CD_BACK_COMMAND)){
				if(dir.startsWith("/")){
					if(new File(dir).isDirectory()){
						if(dir.length() < current_dir.length()){
							dos.writeUTF(CD_ROOT_MESSAGE);
						}else{
							current_dir = dir;
							printWorkingDirectory(dos);
						}
					}else{
						System.out.println("it failed");
						dos.writeUTF(CD_FAILURE_MESSAGE);
					}
				}else{
					if (new File(current_dir+"/"+dir).isDirectory()){
					current_dir = current_dir+"/"+dir;
					System.out.println("directory changed: ");
					printWorkingDirectory(dos);
				}else{
					System.out.println("it failed");
					dos.writeUTF(CD_FAILURE_MESSAGE);
					}
				}
			}else{
				System.out.println("you want to change to: "+current_dir.substring(0,current_dir.lastIndexOf('/')));
				System.out.println("Actual dir is: "+root_dir);
					//if(current_dir.substring(0,current_dir.lastIndexOf('/')).equalsIgnoreCase(root_dir)){
					if(current_dir.equalsIgnoreCase(root_dir)){
						dos.writeUTF(CD_ROOT_MESSAGE);
					}else{
						current_dir = current_dir.substring(0,current_dir.lastIndexOf('/'));
						printWorkingDirectory(dos);
					}
			}
		}catch(Exception e){
			dos.writeUTF(UNEXPECTED_ERROR);
		}
	}//------------------end of changeDirectory()-------------------

	//------------------listSubdirectories in correspondence to the ls command from client-------------------
	public static void listSubdirectories(DataOutputStream dos) throws Exception{
		try{
			File[] fList = new File(current_dir).listFiles();
			if(fList != null && fList.length == 0){
				dos.writeUTF(LS_NO_SUBDIR);
			}else{
				String listOfFiles = "";
				for(File file : fList){
					listOfFiles = listOfFiles+" "+file.getName();
				}
				dos.writeUTF(listOfFiles);
			}
		}catch(Exception e){
			dos.writeUTF(UNEXPECTED_ERROR);
		}
	}//------------------end of listSubdirectories()-------------------

	//------------------delete file on the server-------------------
	public static void deleteFile(DataOutputStream dos, String fileName) throws Exception{
		try{
			if(fileName.startsWith("/")){
				System.out.println("file path is: "+fileName);
				if(new File(fileName).exists()){
					new File(fileName).delete();
					dos.writeUTF(FILE_DELETED);
				}else{
					dos.writeUTF(FILE_NOT_PRESENT);
				}
			}else{
				if(new File(current_dir+"/"+fileName).exists()){
					new File(current_dir+"/"+fileName).delete();
					dos.writeUTF(FILE_DELETED);
				}else{
					dos.writeUTF(FILE_NOT_PRESENT);
				}
			}
		}catch(Exception e){
			dos.writeUTF(UNEXPECTED_ERROR);
		}
	}//------------------end of deleteFile()-------------------

//------------------sendFile in correspondence to the get command from client-------------------
public void sendFile(DataOutputStream dos, DataInputStream dis, Socket s, String fileName){
		try{
        File f=new File(current_dir+"/"+fileName);
        if(!f.exists())
        {
            dos.writeUTF("File Not Found");
						dos.writeUTF("operation Aborted");
            return;
        }
        else
        {
            dos.writeUTF("found");
						if(dis.readUTF().compareTo("Cancel")==0){
							dos.writeUTF("Opertion aborted");
							return;
						}
            FileInputStream fin=new FileInputStream(f);
            int ch;
            do
            {
                ch=fin.read();
                dos.writeUTF(String.valueOf(ch));
            }
            while(ch!=-1);
            fin.close();
            dos.writeUTF("File Received Successfully");
        }
		}catch(Exception e){
			e.printStackTrace();
		}
	}//------------------end of sendFile()-------------------

//------------------receiveFile in correspondence to the put command from client-------------------
 public void receiveFile(DataOutputStream dos, DataInputStream dis, Socket s, String fileName){
	 try{
		 File f=new File(current_dir+"/"+fileName);

		 if(dis.readUTF().compareTo("operation Aborted")==0){
			 // dos.writeUTF("operation Aborted");
			 return;
		 }

		 	if(f.exists()){
			 	dos.writeUTF("File already exists in Server");
				String opt = dis.readUTF();
				if(opt.compareTo("N")==0){
					System.out.println("Not overwritten");
					dos.writeUTF("Aborted operation");
					return;
				}
		 	}
			else
				dos.writeUTF("Sending...");

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
			dos.writeUTF("Transfer complete\nElapsed time: " + (output/1000.0)+"seconds or "+ (output/(1000.0*60))+"minutes");

	 }catch(Exception e){
		 e.printStackTrace();
	 }
 }//------------------end of receiveFile()-------------------
 //--------------------------------run() method is overridden here to execute server tasks------------------------------
	public void run(){
		try{
			// this.s = this.server.accept();
			// Scanner sc = new Scanner(System.in);
			String message = "Chat started!";
			System.out.println("Connected "+this.s);

			DataOutputStream dos=new DataOutputStream(this.s.getOutputStream());		//send message to the Client
			DataInputStream dis=new DataInputStream(this.s.getInputStream());		//get input from the client
			String command = "";

			while(message!="exit"){
				System.out.println("server while loop");
				command = dis.readUTF();
				System.out.println("Command called: " +command);
				if(command.equalsIgnoreCase(PWD_COMMAND)){
					this.printWorkingDirectory(dos);
				}
				else if(command.contains(MKDIR_COMMAND) && command.substring(0,6).equalsIgnoreCase(MKDIR_COMMAND)){
					this.makeDirectory(dos, command);
				}
				else if(command.contains(CD_COMMAND) && command.substring(0,3).equalsIgnoreCase(CD_COMMAND)){
					this.changeDirectory(dos, command.substring(3));
				}
				else if(command.equalsIgnoreCase(LS_COMMAND)){
					System.out.println("checking ls port is -> "+this.portType);
					this.listSubdirectories(dos);
				}
				else if(command.contains(DELETE_COMMAND) && command.substring(0,7).equalsIgnoreCase(DELETE_COMMAND)){
					this.deleteFile(dos, command.substring(7));
				}
				else if(command.contains(GET_COMMAND) && command.substring(0,4).equalsIgnoreCase(GET_COMMAND)){
					int at_end = command.length() - 2;
					if(command.substring(at_end).equals(T_PORT_CALL)){
            Thread t = new tport_get_put(dos, dis, s, command.substring(0, at_end), current_dir);			// create a new thread object on tport
            t.start();	    																// Invoking the start() method
					}
					else
						this.sendFile(dos, dis, s, command.substring(4));
				}
				else if(command.contains(PUT_COMMAND) && command.substring(0,4).equalsIgnoreCase(PUT_COMMAND)){
					this.receiveFile(dos, dis, s, command.substring(4));
				}
				//If terminate command is recieved (it can only be recieved on tport), We do terminate stuff
				else if(command.contains(TERMINATE_COMMAND)){
					System.out.println("Terminate command called "+this.portType);
					System.out.println("Before sending reply");
					//This is just a test message sent back to client. Will be modified later
					dos.writeUTF(TERMINATE_SUCCESSFUL);
					System.out.println("After sending reply");
					//Setting isTerminated as true as an indication that terminate command has been called
					this.isTerminated = true;
					Thread.currentThread().sleep(10);
				}
				else if(command.equalsIgnoreCase(QUIT_COMMAND)){
					dos.writeUTF(QUIT_MESSAGE);
					//break;
					System.out.println(WAITING_MSG);
					this.s=this.server.accept();
					System.out.println("Connected "+s);
					dos=new DataOutputStream(this.s.getOutputStream());		//send message to the Client
					dis=new DataInputStream(this.s.getInputStream());			//get input from the client
				}
				else{
					dos.writeUTF(INVALID_CMD_MESSAGE);
				}
			}
			System.out.println("Server stopped running");
		}catch(Exception e){
			System.out.println("Port is "+this.portType);
			e.printStackTrace();
		}
	}
	//------------------------------run() method ends-------------------------------------------------
	//------------------main method-------------------
	public static void main(String args[]) throws Exception{
		try{
					// This method is modified so that when this class is invoked, it will spawn off
					// two threads listening on nport and tport simultaeously.
					myftpserver nportServer = new myftpserver(Integer.valueOf(args[0]), N_PORT);
					myftpserver tportServer = new myftpserver(Integer.valueOf(args[1]), T_PORT);
					nportServer.start();
					tportServer.start();

		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
/**
This class handles multiple client connections and spawns off new thread for each client
*/
class ClientManager extends Thread{

	public ServerSocket nportServer;
	public ServerSocket tportServer;
	public int nportNumber;
	public int tportNumber;

	public ClientManager(int nportNumber, int tportNumber) {
		this.nportNumber = nportNumber;
		this.tportNumber = tportNumber
	}

	public run() {
		this.nportServer = new ServerSocket(nportNumber);
		this.tportServer = new ServerSocket(tportNumber);
		while(true) {
			try {
				Socket client = this.nportServer.accept();
			} catch(Exception e) {
				System.out.println("Error Connecting to the server")
				e.printStackTrace();
			}
			tport_get_put mainThread = new tport_get_put(this.nportServer, this.tportServer, tportNumber);
		}
	}
	//------------------main method-------------------
	public static void main(String args[]) throws Exception{
		try{
			// This method is modified so that when this class is invoked, it will spawn off
		  // two threads listening on nport and tport simultaeously.
			ClientManager server = new ClientManager(Integer.valueOf(args[0]), Integer.valueOf(args[0]));
			nportServer.start();
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
