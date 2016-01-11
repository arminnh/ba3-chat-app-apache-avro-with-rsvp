package server;

public class Server {
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
			// args[0] is the port number on which the server will run
			System.out.println("Usage: server <port>");
			System.exit(0);
		}
		
		SayHelloObject sayHello = new SayHelloObject();
		SayByeObject sayBye= new SayByeObject();
		
		RemoteReferenceModule rrm = new RemoteReferenceModule();
		rrm.register(sayHello);
		rrm.register(sayBye);
		
		CommunicationModule comm = new CommunicationModule(rrm, new DispatchingModule());
		System.out.println("CommunicationModule start()");
		comm.start(Integer.parseInt(args[0]));
	}
}
