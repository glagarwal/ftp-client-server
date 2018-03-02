import java.net.*;
import java.io.*;
import java.util.*;

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
  public Socket nportSocket;
	public ServerSocket tportServer;
  public Socket tportSocket;

  // To identify in which context the thread is runnning, we create below flags and assign them to
  public boolean threadContextNport = false;
  public boolean threadContextTport = false;
  public boolean threadContextGet = false;
  public boolean threadContextPut = false;

  public static DataOutputStream dos;
  public static DataInputStream dis;
	//GET and PUT threads use this variables
  //public DataOutputStream dos_get;
  //public DataInputStream dis_get;
  public String fileNameGet = "";
  public String currentDirGet = "";

  public DataOutputStream dos_put;
  public DataInputStream dis_put;
  public String fileNamePut = "";
  public String currentDirPut = "";

  public static Map<Long, Boolean> terminateMap= new HashMap<Long, Boolean>();

	//----------------------Constructor to instantiate myftpserver nport thread ------------------------------
	myftpserver(Socket nportSocket, ServerSocket tportServer, boolean threadContextNport){
		try{
      System.out.println("In nport constructor");
      this.threadContextNport = threadContextNport;
			this.nportSocket = nportSocket;
      this.tportServer = tportServer;
		}catch(Exception e){
			e.printStackTrace();
		}
	}//------------------end of Constructor---------------------
  //----------------------Constructor to instantiate myftpserver tport thread ----------------------------------
	myftpserver(Socket tportSocket, boolean threadContextTport){
		try{
      this.threadContextTport = threadContextTport;
      this.tportSocket = tportSocket;
		}catch(Exception e){
			e.printStackTrace();
		}
	}//------------------end of Constructor---------------------
  //----------------------Constructor to instantiate myftpserver get thread ----------------------------------
  myftpserver(Socket nportSocket, DataOutputStream dos, DataInputStream dis, String fileName, String current_dir, boolean isThreadContextGet){
    try{
      if(isThreadContextGet){
        this.nportSocket = nportSocket;
        this.threadContextGet = isThreadContextGet;
        //this.dos_get = dos;
        //this.dis_get = dis;
        this.fileNameGet = fileName;
        this.currentDirGet = current_dir;
      }
      else{
        this.threadContextPut = !isThreadContextGet;
        //this.dos_put = dos;
        //this.dis_put = dis;
        this.fileNamePut = fileName;
        this.currentDirPut = current_dir;
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }//------------------end of Constructor---------------------
  //-------------method to change terminateMap values-----------------
  public static synchronized boolean mapMethods(long threadId, boolean isTerminate, String operation){
    if(operation.equalsIgnoreCase("set")){
      terminateMap.put(threadId, isTerminate);
      return true;
    }
    else if(operation.equalsIgnoreCase("get")){
      return terminateMap.get(threadId);
    }
    else if(operation.equalsIgnoreCase("remove")){
      terminateMap.remove(threadId);
      return true;
    }
    return false;
  }
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
public void sendFile(DataOutputStream dos, DataInputStream dis, String fileName){
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
 public void receiveFile(DataOutputStream dos, DataInputStream dis, String fileName){
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
      System.out.println("In run");
			// this.s = this.server.accept();
			// Scanner sc = new Scanner(System.in);
      if(this.threadContextNport){
        System.out.println("In nport loop ");
        String message = "Chat started!";
  			System.out.println("Connected nport "+this.nportSocket);
        Socket tportSocket = this.tportServer.accept();
        myftpserver tportThread = new myftpserver(tportSocket, true);
        tportThread.start();

        dos=new DataOutputStream(this.nportSocket.getOutputStream());		//send message to the Client
  			dis=new DataInputStream(this.nportSocket.getInputStream());		//get input from the client
  			String command = "";

        while(message!="exit"){
  				System.out.println("server while loop");
  				command = this.dis.readUTF();
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
  					this.listSubdirectories(dos);
  				}
  				else if(command.contains(DELETE_COMMAND) && command.substring(0,7).equalsIgnoreCase(DELETE_COMMAND)){
  					this.deleteFile(dos, command.substring(7));
  				}
  				else if(command.contains(GET_COMMAND) && command.substring(0,4).equalsIgnoreCase(GET_COMMAND)){
  					if(command.charAt(command.length()-1) == '&'){
              System.out.println("In get & loop");
              myftpserver getThread = new myftpserver(nportSocket, dos, dis, command.split(" ")[1], current_dir, true);			// create a new thread object on tport
              getThread.start();
              dos.writeUTF(Long.toString(getThread.getId()));    																                                  // Invoking the start() method
              boolean dummy = mapMethods(getThread.getId(), false, "set");
  					}
  					else{
              this.sendFile(dos, dis, command.substring(4));
            }
  				}
  				else if(command.contains(PUT_COMMAND) && command.substring(0,4).equalsIgnoreCase(PUT_COMMAND)){
  					this.receiveFile(dos, dis, command.substring(4));
  				}
  				else if(command.equalsIgnoreCase(QUIT_COMMAND)){
  					dos.writeUTF(QUIT_MESSAGE);
  					//break;
  					System.out.println(WAITING_MSG);
  					//this.nportSocket = this.server.accept();
  					System.out.println("Connected "+nportSocket);
  					dos=new DataOutputStream(this.nportSocket.getOutputStream());		//send message to the Client
  					dis=new DataInputStream(this.nportSocket.getInputStream());			//get input from the client
  				}
  				else{
  					dos.writeUTF(INVALID_CMD_MESSAGE);
  				}
  			}
      }
      else if(this.threadContextTport){

        String message = "Chat started!";
        System.out.println("Connected tport "+this.tportSocket);
        DataOutputStream dos=new DataOutputStream(this.tportSocket.getOutputStream());		//send message to the Client
  			DataInputStream dis=new DataInputStream(this.tportSocket.getInputStream());		//get input from the client
  			String command = "";

        while(!message.equalsIgnoreCase("exit")){
          command = dis.readUTF();
  				System.out.println("Command called: " +command);
          if(command.contains(TERMINATE_COMMAND)){
            //Terminate here
            boolean dummy = mapMethods(Long.parseLong(command.split(" ")[1]), true, "set");
          }
        }
      }
      else if(this.threadContextGet){
        System.out.println("In get thread");
        System.out.println("current directory: " +this.currentDirGet+"file get name "+this.fileNameGet);
        File f=new File(this.currentDirGet+"/"+fileNameGet);
        if(!f.exists())
        {
            System.out.println("File does not exists");
            dos.writeUTF("File Not Found");
						dos.writeUTF("operation Aborted");
            return;
        }
        else
        {
            System.out.println("File exists yeyyyy!!!!!!!!");
            System.out.println("dos is "+dos.size());
            dos.writeUTF("found");
            System.out.println("After writting");
						if(dis.readUTF().compareTo("Cancel")==0){
							dos.writeUTF("Opertion aborted");
							return;
						}
            FileInputStream fin=new FileInputStream(f);
            int ch = 0;
            for(int i = 0; ch != -1; i++){
              if(i%1000 == 0 && mapMethods(this.currentThread().getId(), false, "get")){
                dos.writeUTF("Please delete the file requested");
                ch = -1;
                break;
              }else{
                ch=fin.read();
                dos.writeUTF(String.valueOf(ch));
              }
            }
            fin.close();
            dos.writeUTF("File Received Successfully");
        }
      }
			System.out.println("Server stopped running");

		}catch(Exception e){
			//System.out.println("context is "+this.threadContext);
			e.printStackTrace();
		}
	}
	//------------------------------run() method ends-------------------------------------------------
}
/**
This class handles multiple client connections and spawns off new thread for each client
*/
class ClientManager extends Thread{

	public ServerSocket nportServer;
	public ServerSocket tportServer;
	public int nportNumber;
	public int tportNumber;
  public static final String NPORT_CONTEXT = "nport_context";
  public static final String UNEXPECTED_ERROR = "Unexpected error occured";

	public ClientManager(int nportNumber, int tportNumber) {
		this.nportNumber = nportNumber;
		this.tportNumber = tportNumber;
	}

	public void run() {
    try{
      this.nportServer = new ServerSocket(nportNumber);
  		this.tportServer = new ServerSocket(tportNumber);

      while(true) {
        Socket nportClientSocket = null;
  			try {
          System.out.println("Server will start to wait" + this.nportServer);
  				nportClientSocket = this.nportServer.accept();
  			} catch(Exception e) {
  				System.out.println("Error Connecting to the server");
  				e.printStackTrace();
  			}
        System.out.println("Server connected "+nportClientSocket);
  			myftpserver mainThread = new myftpserver(nportClientSocket, this.tportServer, true);
        mainThread.start();
  		}
    }catch(Exception e){
      e.printStackTrace();
    }
	}
	//------------------main method-------------------
	public static void main(String args[]) throws Exception{
		try{
			// This method is modified so that when this class is invoked, it will spawn off
		  // two threads listening on nport and tport simultaeously.
			ClientManager clientManager = new ClientManager(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
			clientManager.start();
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
