package client;
import message.*;
import java.net.*;

public class Client {

	public static void main(String[] args) {
		try {			
			InetAddress addr = InetAddress.getByName(args[0]);
			Socket socket = new Socket(addr, Integer.parseInt(args[1]));
			CommunicationModule comm = CommunicationModule.createCommunicationModule(socket);
			
			//Hier Hello World proxy object
			
			ReplyMessage result = comm.remoteInvocation(new Proxy() /* proxy */, method /* SayHello */, args /* Josse */);
			//Haal data replymessage

		} catch (UnknownHostException e) {
            System.err.println("Don't know about host " + args[0]);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
			e.printStackTrace();
		}

	}

}
