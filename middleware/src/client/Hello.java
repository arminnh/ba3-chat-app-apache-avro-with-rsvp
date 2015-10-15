package client;
import server.Server;
import server.RemoteReferenceModule;

public class Hello {
	public static void main(String[] args) {
		Server server = new server.Server();
		System.out.println(server.sayHello("blablabla"));
		 
		RemoteReferenceModule remoote = new server.RemoteReferenceModule();
		remoote.register(new String("serhsgrhsgrsgrsgrsgrsgrhefio;eh"));
		remoote.register(new Integer(5));
		System.out.println(remoote.retrieve("String"));
		System.out.println(remoote.retrieve("Integer"));
	}
}
