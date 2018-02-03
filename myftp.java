import java.net.*;
import java.io.*;
import java.util.*;

class myftp{

	public static int maxFileSize = 999999999;
	public static final String GET_COMMAND = "get";
	public static final String download_dir = System.getProperty("user.dir").concat("/client/downloads");
	public static int byteRead;

	public static void main(String args[]){
		try{
			Socket s=new Socket("127.0.0.1",3000);
			DataInputStream dis=new DataInputStream(s.getInputStream());	//client
			DataOutputStream dos=new DataOutputStream(s.getOutputStream());
			Scanner sc=new Scanner(System.in);
			String command = "Chat started!";
			String msg = "G";

			while(true){		//!msg.equalsIgnoreCase("exit") && !command.equalsIgnoreCase("exit")
				//System.out.println(msg);
				System.out.print("Enter command: ");
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
				if(command.equalsIgnoreCase("quit")){
					break;
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
