package client;
import message.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;

public class CommunicationModule {
	private Socket socket = null;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private MiddlewareInvocationHandler invocationHandler;
	private Class<?> theClass = null;
	
	CommunicationModule(Socket socket) {
		this.invocationHandler = new MiddlewareInvocationHandler(this);
		this.socket = socket;
		try {
			this.out = new ObjectOutputStream( socket.getOutputStream() );
			this.in = new ObjectInputStream( socket.getInputStream() );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ReplyMessage run(RequestMessage requestMessage) {
		ReplyMessage replyMessage = null;
		
        try {
        	out.writeObject(requestMessage);
        	replyMessage = (ReplyMessage) in.readObject();
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
