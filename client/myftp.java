import java.net.*;
import java.io.*;
import java.util.*;

class myftp{

	// public static int maxFileSize = 999999999;
	public static final String GET_COMMAND = "get";
	public static final String PUT_COMMAND = "put";
	public static final String UNEXPECTED_ERROR = "Unexpected error occured";
	public static final String download_dir = System.getProperty("user.dir");		//.concat("/client");
	public static DataInputStream dis;
	public static DataOutputStream dos;
	public static Scanner sc=new Scanner(System.in);
	// public static int byteRead;

	public static void main(String args[]){
		try{
			Socket s=new Socket(args[0],Integer.valueOf(args[1]));
			dis=new DataInputStream(s.getInputStream());	//get input from the server
			dos=new DataOutputStream(s.getOutputStream());	//send message to the server
			String command = "Chat started!";
			String msg = "G";

			while(true){		//!msg.equalsIgnoreCase("exit") && !command.equalsIgnoreCase("exit")
				//System.out.println(msg);
				System.out.print("mytftp> ");
				command = sc.nextLine();
				dos.writeUTF(command);
				if(command.contains(GET_COMMAND) && command.substring(0,3).equalsIgnoreCase(GET_COMMAND)){
					//dos.writeUTF(command);
					get(command, s);
				}
				else if(command.contains(PUT_COMMAND) && command.substring(0,3).equalsIgnoreCase(PUT_COMMAND)){
					File f=new File(command.substring(4));
					if(!f.exists())
	        {
	            System.out.println("File Not Found on your Local machine!");
							continue;
					}
					put(command, s);
				}
				msg = dis.readUTF();
				System.out.println("Reply: " +msg);

				if(command.equalsIgnoreCase("quit")){
					break;
				}
			}
		} catch(Exception e){
			System.out.println(UNEXPECTED_ERROR+": "+e);
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
				//String repFromServer=dis.readUTF();
        File f=new File(fileName);
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
}
