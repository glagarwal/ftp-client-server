import java.net.*;
import java.io.*;
import java.util.*;

class myftpserver{
	public static final String root_dir = System.getProperty("user.dir").concat("/myftpserver");
	public static String current_dir = root_dir;
	public static final String PWD_COMMAND = "pwd";

	public static final String MKDIR_COMMAND = "mkdir";
	public static final String MKDIR_SUCCESS_MESSAGE = "directory created";

	public static final String CD_FAILURE_MESSAGE = "directory does not exist";
	public static final String CD_COMMAND = "cd";
	public static final String CD_SUCCESS_MESSAGE = "directory changed to ";
	public static final String CD_BACK_COMMAND = "..";
	public static final String CD_ROOT_MESSAGE = "You are already at root directory";

	public static final String LS_COMMAND = "ls";
	public static final String LS_NO_SUBDIR = "No files or subdirectories";

	public static final String DELETE_COMMAND = "delete";
	public static final String FILE_NOT_PRESENT = "File does not exist";
	public static final String FILE_DELETED = "File deleted";

	public static final String GET_COMMAND = "get";

	public static void printWorkingDirectory(DataOutputStream dos){
		try{
				dos.writeUTF(current_dir);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	public static void makeDirectory(DataOutputStream dos, String dir_name){
		try{
			File dir = new File(current_dir.concat("/").concat(dir_name.substring(6)));
			dir.mkdirs();
			dos.writeUTF(MKDIR_SUCCESS_MESSAGE);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void changeDirectory(DataOutputStream dos, String dir){
		try{
			if(!dir.equalsIgnoreCase(CD_BACK_COMMAND)){
				if (new File(current_dir+"/"+dir).isDirectory()){
					current_dir = current_dir+"/"+dir;
					System.out.println("directory changed: "+current_dir);
					dos.writeUTF(CD_SUCCESS_MESSAGE+current_dir);
				}else{
					System.out.println("it failed");
					dos.writeUTF(CD_FAILURE_MESSAGE);
				}
			}else{
				System.out.println("you want to change to: "+current_dir.substring(0,current_dir.lastIndexOf('/')));
				System.out.println("Actual dir is: "+root_dir);
					//if(current_dir.substring(0,current_dir.lastIndexOf('/')).equalsIgnoreCase(root_dir)){
					if(current_dir.equalsIgnoreCase(root_dir)){
						dos.writeUTF(CD_ROOT_MESSAGE);
					}else{
						current_dir = current_dir.substring(0,current_dir.lastIndexOf('/'));
						dos.writeUTF(current_dir);
					}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void listSubdirectories(DataOutputStream dos){
		try{
			File[] fList = new File(current_dir).listFiles();
			if(fList.length == 0){
				dos.writeUTF(LS_NO_SUBDIR);
			}else{
				String listOfFiles = "";
				for(File file : fList){
					listOfFiles = listOfFiles+" "+file.getName();
				}
				dos.writeUTF(listOfFiles);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void deleteFile(DataOutputStream dos, String fileName){
		try{
			if(new File(current_dir+"/"+fileName).exists()){
				new File(current_dir+"/"+fileName).delete();
				dos.writeUTF(FILE_DELETED);
			}else{
				dos.writeUTF(FILE_NOT_PRESENT);
			}
		}catch(Exception e){

		}
	}

	public static void getFile(DataOutputStream dos, Socket s, String fileName){
		try{
			if(new File(current_dir+"/"+fileName).exists()){
				System.out.println("file exists");

				File fileToSend = new File(current_dir+"/"+fileName);
				byte[] buffer = new byte[(int) fileToSend.length()];
				System.out.println("size of file is:"+fileToSend.length());
				FileInputStream fs = new FileInputStream(fileToSend);
				BufferedInputStream bis = new BufferedInputStream(fs);
				bis.read(buffer, 0, buffer.length);
				dos.write(buffer, 0, buffer.length);

			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String args[]){
		try{
			ServerSocket server=new ServerSocket(3000);
			System.out.println("Server started");
			Socket s=server.accept();

			Scanner sc = new Scanner(System.in);
			String message = "Chat started!";
			System.out.println("Connected "+s);

			DataOutputStream dos=new DataOutputStream(s.getOutputStream());		//server
			DataInputStream dis=new DataInputStream(s.getInputStream());
			//dos.writeUTF("----Welcome to Server at port 3000 -- "+server+"----");
			String rec = "S";
			while(message!="exit" && rec!="exit"){
				rec = dis.readUTF();
				System.out.println("Command called: " +rec);
				if(rec.equalsIgnoreCase(PWD_COMMAND)){
				  printWorkingDirectory(dos);
				}
				else if(rec.contains(MKDIR_COMMAND) && rec.substring(0,5).equalsIgnoreCase(MKDIR_COMMAND)){
					makeDirectory(dos, rec);
				}
				else if(rec.contains(CD_COMMAND) && rec.substring(0,2).equalsIgnoreCase(CD_COMMAND)){
					changeDirectory(dos, rec.substring(3));
				}
				else if(rec.equalsIgnoreCase(LS_COMMAND)){
					listSubdirectories(dos);
				}
				else if(rec.contains(DELETE_COMMAND) && rec.substring(0,6).equalsIgnoreCase(DELETE_COMMAND)){
					deleteFile(dos, rec.substring(7));
				}
				else if(rec.contains(GET_COMMAND) && rec.substring(0,3).equalsIgnoreCase(GET_COMMAND)){
					getFile(dos, s, rec.substring(4));
				}
			}
			System.out.println("Server stopped");
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
