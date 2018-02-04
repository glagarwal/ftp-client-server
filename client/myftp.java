import java.net.*;
import java.io.*;
import java.util.*;

class myftp{

	public static int maxFileSize = 999999999;
	public static final String GET_COMMAND = "get";
	public static final String download_dir = System.getProperty("user.dir").concat("/downloads");
	public static int byteRead;
	public static final String QUIT_COMMAND = "quit";
	public static final String UNEXPECTED_ERROR = "Unexpected error occured";

	public static void main(String args[]) throws Exception{
		try{
				Socket s=new Socket(args[0],Integer.valueOf(args[1]));
				DataInputStream dis=new DataInputStream(s.getInputStream());	//client
				DataOutputStream dos=new DataOutputStream(s.getOutputStream());
				Scanner sc=new Scanner(System.in);
				String command = "Chat started!";
				String msg = "";

				//This creates a directory called client/downloads for user. User will always be under this directory
				if(!(new File(System.getProperty("user.dir").concat("/downloads"))).isDirectory()){
					File userContext = new File(System.getProperty("user.dir").concat("/downloads"));
					userContext.mkdirs();
				}

				while(true){		//!msg.equalsIgnoreCase("exit") && !command.equalsIgnoreCase("exit")
					//System.out.println(msg);
					System.out.print("mytftp> ");
					command = sc.nextLine();
					if(command.contains(GET_COMMAND) && command.substring(0,3).equalsIgnoreCase(GET_COMMAND)){
						dos.writeUTF(command);
						byte[] buffer = new byte[maxFileSize];
						File file = new File(download_dir+"/"+command.substring(4));
						//file.createNewFile();
						FileOutputStream fos = new FileOutputStream(file);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						System.out.println("before while loop");

						while((byteRead = dis.read(buffer, 0, buffer.length)) != -1){
							System.out.println("in while loop");
							System.out.println(byteRead);
							bos.write(buffer, 0, byteRead);
						}
					}else{
						dos.writeUTF(command);
					}
					msg = dis.readUTF();
					System.out.println("Reply: " +msg);
					if(command.equalsIgnoreCase(QUIT_COMMAND)){
						break;
					}
				}
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
		}
	}
}
