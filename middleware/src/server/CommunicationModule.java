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
        	System.out.println("Listening on port " + port);
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
}
