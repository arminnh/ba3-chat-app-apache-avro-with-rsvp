package server;
import java.net.*;
import java.io.*;

public class CommunicationModule {
	//Dispatching and remote reference module
	private RemoteReferenceModule rrm = new RemoteReferenceModule();
	private DispatchingModule dispatcher = new DispatchingModule();

	void start(int port) {
		/*while (true) {
		   accept a connection;
		   create a thread to deal with the client;
		}*/
		
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (listening) {
	            new Thread(new CommunicationAnalyzer(serverSocket.accept(), this.rrm, this.dispatcher)).start();
	        }
	    } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }
    }

	//constructors
	CommunicationModule() { }
	CommunicationModule(RemoteReferenceModule rrm, DispatchingModule dispatcher) {
		if (rrm != null) this.rrm = rrm;
		if (dispatcher != null) this.dispatcher = dispatcher;
	}
	//getters and setters

	//placeholder for the implementation of the handling of an incoming request
	void handleRequest() {}

	/*
	void voorbeeld() {

		//hostname = ip-address

		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);

		try (
				Socket echoSocket = new Socket(hostName, portNumber);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader( new InputStreamReader(echoSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader( ew InputStreamReader(System.in))
				) {
			String userInput;
			while ((userInput = stdIn.readLine()) != null) {
				out.println(userInput);
				System.out.println("echo: " + in.readLine());
			}
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostName);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " +
					hostName);
			System.exit(1);
		} 
	}
	*/
}

//Two classes make the connection:
//Socket
//ServerSocket


