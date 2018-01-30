import java.net.*;
import java.io.*;
import java.util.*;

class myftpserver{
	public static void main(String args[]){
		try{
			ServerSocket server=new ServerSocket(3000);
			System.out.println("Server started");
			Socket s=server.accept();

			Scanner sc=new Scanner(System.in);
			String message = "Chat started!";
			System.out.println("Connected "+s);

			DataOutputStream dos=new DataOutputStream(s.getOutputStream());		//server
			DataInputStream dis=new DataInputStream(s.getInputStream());
			//dos.writeUTF("----Welcome to Server at port 3000 -- "+server+"----");			
			String rec = "S";
			while(message!="exit" && rec!="exit"){
				System.out.print("Enter text: ");
				message = sc.nextLine();
				dos.writeUTF(message);
				rec = dis.readUTF();
				System.out.println("Reply: " +rec);
			}
			System.out.println("Server stopped");

		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
