(a) Group Members:
		Gaurav L Agarwal
		Ankit Vaghela


(b) Compilation/execution steps:
		In order to test/execute this project below steps are required (in order):

		1. Navigate to directory named server and compile server file using below command:

			javac myftpserver.java

		2. Run server using below command:

			java myftpserver <NPORT NUMBER> <TPORT NUMBER>

			ex. java myftpserver 3000 4000

		3. Navigate to directory named client and compile client file using below command:

			javac myftp.java

		4. Run the client using below command:

			java myftp <SERVER MACHINE NETWORK ADDRESS> <NPORT NUMBER> <TPORT NUMBER>

			ex. java myftp localhost 3000 4000
				java myftp 127.0.0.1 3000 4000
				
		{Note: we have made a provision for the "cd" command to not go beyond the root directory where the program resides.
			To quit the server, press "ctrl+c" on the terminal where the server is running.

			After a get / put command with "&", you'll be able to only run "terminate <COMMAND ID>" command. Other commands will not be accepted till the get/put thread is either terminated or completes execution by itself.}


(c) This project was done in its entirety by Gaurav Agarwal & Ankit Vaghela. We hereby state that we have not received unauthorized help of any form.
