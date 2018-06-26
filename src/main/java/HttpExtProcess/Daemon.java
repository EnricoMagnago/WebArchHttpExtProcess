package HttpExtProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Project: HttpExtProcess
 * Created by en on 18/09/17.
 */

public class Daemon{
	/**
	 * main : tries to bind a socket to the specified port;
	 * interaction with clients handled by HttpExtProcess.Connection.
	 * exits with 1 if there has been an error in parsing the port given in the command line;
	 * exits with 2 if there has been an error in the binding of the socket;
	 *
	 * @param argv argv[0] is an optional parameter and allows to specify the port.
	 */
	public static void main(String argv[]){
		int port = 8000;
		// parse port if specified.
		if(argv.length > 0){
			try{
				port = Integer.parseInt(argv[0]);
			}catch(NumberFormatException exc){
				System.err.println("Could not parse port number: " + argv[0]);
				System.exit(1);
			}
		}
		ServerSocket ss = null;
		try{
			ss = new ServerSocket(port);
		}catch(IOException exc){
			System.err.println("could not bind server socket to port: " + port);
			System.exit(2);
		}

		System.out.println("server daemon started correctly, please stop with ctrl+C");
		Socket incomingRequest;
		while(true){
			try{
				incomingRequest = ss.accept();
				// no exception raised, go on and handle interaction.
				new Connection(incomingRequest);
			}catch(IOException exc){
				System.err.println("IOException in accepting request: " + exc);
				// do not stop server for 1 "bad request".
			}
		}
	}
}
