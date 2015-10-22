package server;
import java.net.*;
import java.io.*;

public class CommunicationModule {
	private RemoteReferenceModule rrm = new RemoteReferenceModule();
	private DispatchingModule dispatcher = new DispatchingModule();

	void start(int port) {		
		//accept a connection;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
        	System.out.println("Listening on port " + port + "\n\n");
            while (true) {
            	//create a thread to deal with the client;
            	CommunicationAnalyzer coma = new CommunicationAnalyzer(serverSocket.accept(), this.rrm, this.dispatcher);
            	System.out.println("New connection accepted");
	            new Thread(coma).start();
	        }
	    } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }
    }

	CommunicationModule() { }
	CommunicationModule(RemoteReferenceModule rrm, DispatchingModule dispatcher) {
		if (rrm != null) this.rrm = rrm;
		if (dispatcher != null) this.dispatcher = dispatcher;
	}
}
