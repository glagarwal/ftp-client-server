import java.net.*;
import java.io.*;
import java.util.*;

class myftp{

	public static int maxFileSize = 999999999;
	public static final String GET_COMMAND = "get";
	public static final String download_dir = System.getProperty("user.dir").concat("/client/downloads");
	// public static int byteRead;

	public static void receive(String command){
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
				if(f.exists())
				{
						// String Option;
						System.out.print("File Already Exists. Want to OverWrite (Y/N) ?	");
						String Option=sc.nextLine();
						if(Option=="N")
						{
								dos.flush();
								return;
						}
				}
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
				//System.out.println(dis.readUTF());
				System.out.println("Elapsed time: " + (output/1000.0)+"seconds or "+ (output/(1000.0*60))+"minutes");
			}
	}

	public static void main(String args[]){
		try{
			Socket s=new Socket("127.0.0.1",3000);
			DataInputStream dis=new DataInputStream(s.getInputStream());	//get input from the server
			DataOutputStream dos=new DataOutputStream(s.getOutputStream());	//send message to the server
			Scanner sc=new Scanner(System.in);
			String command = "Chat started!";
			// String msg = "G";

			while(true){		//!msg.equalsIgnoreCase("exit") && !command.equalsIgnoreCase("exit")
				//System.out.println(msg);
				System.out.print("Enter command: ");
				command = sc.nextLine();
				if(command.contains(GET_COMMAND) && command.substring(0,3).equalsIgnoreCase(GET_COMMAND)){
					dos.writeUTF(command);
					receive(command);
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
