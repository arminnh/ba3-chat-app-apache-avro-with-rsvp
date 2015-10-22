package server;

public class Server {
	
	public static void main(String[] args) {
		// args[0] is the port number on which the server will run
		
		if (args.length != 1) {
			System.out.println("Usage: server <port>");
			System.exit(0);
		}
		
		SayHelloObject sayHello = new SayHelloObject();
		
		RemoteReferenceModule rrm = new RemoteReferenceModule();
		rrm.register(sayHello);
		
		CommunicationModule comm = new CommunicationModule(rrm, new DispatchingModule());
		System.out.println("comm.start()");
		comm.start(Integer.parseInt(args[0]));
		
		System.out.println("Done");
	}
}
