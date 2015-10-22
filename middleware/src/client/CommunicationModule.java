package client;
import message.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

public class CommunicationModule {
	private Socket socket = null;
	private MiddlewareInvocationHandler invocationHandler;
	private Class<?> theClass = null;
	
	CommunicationModule() {
		this.invocationHandler = new MiddlewareInvocationHandler(this);
	}
	
	CommunicationModule(Socket socket) {
		this();
		this.socket = socket;
	}
	
	public ReplyMessage run(RequestMessage requestMessage) {
		ReplyMessage replyMessage = null;
		
        try {
        	ObjectOutputStream out = new ObjectOutputStream( socket.getOutputStream() );
        	ObjectInputStream in = new ObjectInputStream( socket.getInputStream() );
        	
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
			paramTypes[i] = args[i].getClass();
		}
		
		RequestMessage requestMessage = new RequestMessage("from: me", this.theClass.getName(), method.getName(), paramTypes, args);
		//to = "SayHelloObject"
		//public RequestMessage(String from, String to, String methodName, Class<?>[] paramTypes, Object[] paramValues)
		
		ReplyMessage replyMessage = this.run(requestMessage);
		
		return replyMessage.object;
	}
	
	public MiddlewareInvocationHandler invocationHandler() {
		return this.invocationHandler;
	}
	
	public void setClass(Class<?> theClass) {
		this.theClass = theClass;
	}
}
