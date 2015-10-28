package client;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import interfaces.*;

public class Client {

	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: server <address> <port>");
			System.exit(0);
		}
		
		try {
			InetAddress addr = InetAddress.getByName(args[0]);
			Socket socket = new Socket(addr, Integer.parseInt(args[1]));
			CommunicationModule comm = new CommunicationModule(socket);
			
			// create proxy
			SayHelloObjectInterface sayHelloProxy = (SayHelloObjectInterface) ProxyLookup.lookup(SayHelloObjectInterface.class, comm);
			SayByeObjectInterface sayByeProxy = (SayByeObjectInterface) ProxyLookup.lookup(SayByeObjectInterface.class, comm);
			
			System.out.println(sayHelloProxy.sayHello("Josse"));
			System.out.println(sayHelloProxy.add(2, 3));
			
			System.out.println(sayByeProxy.sayBye("Josse"));
			
			socket.close();
			
		} catch (UnknownHostException e) {
            System.err.println("Don't know about host " + args[0]);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
			e.printStackTrace();
		}

	}

}
