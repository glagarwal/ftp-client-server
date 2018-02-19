import java.net.*;
import java.io.*;
import java.util.*;

class myftpserver{
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

	//------------------main method-------------------
	public static void main(String args[]) throws Exception{
		try{

				ServerSocket server=new ServerSocket(Integer.valueOf(args[0]));
				System.out.println("Server started");
				System.out.println(WAITING_MSG);
				Socket s=server.accept();
				Scanner sc = new Scanner(System.in);
				String message = "Chat started!";
				System.out.println("Connected "+s);

				myftpserver mfs = new myftpserver();

				DataOutputStream dos=new DataOutputStream(s.getOutputStream());		//send message to the Client
				DataInputStream dis=new DataInputStream(s.getInputStream());		//get input from the client
				String command = "";

				while(message!="exit"){
					command = dis.readUTF();
					System.out.println("Command called: " +command);
					if(command.equalsIgnoreCase(PWD_COMMAND)){
					  printWorkingDirectory(dos);
					}
					else if(command.contains(MKDIR_COMMAND) && command.substring(0,6).equalsIgnoreCase(MKDIR_COMMAND)){
						makeDirectory(dos, command);
					}
					else if(command.contains(CD_COMMAND) && command.substring(0,3).equalsIgnoreCase(CD_COMMAND)){
						changeDirectory(dos, command.substring(3));
					}
					else if(command.equalsIgnoreCase(LS_COMMAND)){
						listSubdirectories(dos);
					}
					else if(command.contains(DELETE_COMMAND) && command.substring(0,7).equalsIgnoreCase(DELETE_COMMAND)){
						deleteFile(dos, command.substring(7));
					}
					else if(command.contains(GET_COMMAND) && command.substring(0,4).equalsIgnoreCase(GET_COMMAND)){
						mfs.sendFile(dos, dis, s, command.substring(4));
					}
					else if(command.contains(PUT_COMMAND) && command.substring(0,4).equalsIgnoreCase(PUT_COMMAND)){
						mfs.receiveFile(dos, dis, s, command.substring(4));
					}
					else if(command.equalsIgnoreCase(QUIT_COMMAND)){
						dos.writeUTF(QUIT_MESSAGE);
						//break;
						System.out.println(WAITING_MSG);
						s=server.accept();
						System.out.println("Connected "+s);
						dos=new DataOutputStream(s.getOutputStream());		//send message to the Client
						dis=new DataInputStream(s.getInputStream());			//get input from the client
					}
					else{
						dos.writeUTF(INVALID_CMD_MESSAGE);
					}
				}
				System.out.println("Server stopped");

		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
