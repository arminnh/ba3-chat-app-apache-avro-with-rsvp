package client;
import message.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

public class CommunicationModule {
	public Socket socket = null;

	CommunicationModule() { }
	CommunicationModule(String ipadres, int port) { }
	
	public static CommunicationModule createCommunicationModule(Socket socket) {
		CommunicationModule comm = new CommunicationModule();
		
		comm.socket = socket;
		
		return comm;
	}
	
	public ReplyMessage run(RequestMessage requestMessage/* welke functie, welk object, ... in een request message */) {
		ReplyMessage replyMessage = null;
		
        try {
        	ObjectOutputStream out = new ObjectOutputStream( socket.getOutputStream() );
        	ObjectInputStream in = new ObjectInputStream( socket.getInputStream() );
        	
        	Object object = null;
        	//RequestMessage requestMessage = null;
        	//RequesMessage invullen

        	out.writeObject(requestMessage);
        	replyMessage = (ReplyMessage) in.readObject();
        	
        	socket.close();
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + this.socket.getInetAddress());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        return replyMessage;
	}
	
	public Object remoteInvocation(Object proxy, Method method, Object[] args) {
		Class<?>[] paramTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			paramTypes[i] = args.getClass();
		}
		
		RequestMessage requestMessage = new RequestMessage("from", "to", method.getName(), paramTypes, args);
		//to = "SayHelloObject"
		//public RequestMessage(String from, String to, String methodName, Class<?>[] paramTypes, Object[] paramValues)
		
		return this.run(requestMessage);
	}
	
}
