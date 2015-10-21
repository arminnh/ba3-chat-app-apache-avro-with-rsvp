package client;
import message.*;
import java.net.*;
import java.lang.reflect.*;
import server.*; // for SayHelloObject

public class Client {

	public static void main(String[] args) {
		try {			
			InetAddress addr = InetAddress.getByName(args[0]);
			Socket socket = new Socket(addr, Integer.parseInt(args[1]));
			CommunicationModule comm = CommunicationModule.createCommunicationModule(socket);
			
			//Hier Hello World proxy object
			
			Class<?> sayHelloObjectClass = Class.forName("server.SayHelloObject");
			Method sayHelloMethod = sayHelloObjectClass.getMethod("sayHello", new Class<?>[] { "".getClass() });
			Object sayHelloProxy = ProxyLookup.lookup(sayHelloObjectClass, comm);
			Object[] invocationArgs = new Object[] {"Armin"};
			
			ReplyMessage result = (ReplyMessage) comm.remoteInvocation(sayHelloProxy, sayHelloMethod, invocationArgs);
			//Haal data replymessage

			System.out.println(result.object);
			
		} catch (UnknownHostException e) {
            System.err.println("Don't know about host " + args[0]);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
			e.printStackTrace();
		}

	}

}
