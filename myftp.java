import java.net.*;
import java.io.*;
import java.util.*;

class myftp{
	public static void main(String args[]){
		try{
			Socket s=new Socket("192.168.50.198",3000);
			DataInputStream dis=new DataInputStream(s.getInputStream());	//client
			DataOutputStream dos=new DataOutputStream(s.getOutputStream());
			Scanner sc=new Scanner(System.in);
			String send = "Chat started!";
			String msg = "G";
			while(msg!="exit" && send!="exit"){
				//System.out.println(msg);
				System.out.print("Enter text: ");
				send = sc.nextLine();
				dos.writeUTF(send);
				msg = dis.readUTF();
				System.out.println("Reply: " +msg);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
