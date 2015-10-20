package server;
import java.net.*;
import java.io.*;
import message.*;

public class CommunicationAnalyzer implements Runnable {
    private Socket socket = null;
    private RemoteReferenceModule rrm = null; 
    private DispatchingModule dispatcher = null;

    public CommunicationAnalyzer(Socket socket, RemoteReferenceModule rrm, DispatchingModule dispatcher) {
        this.socket = socket;
        this.rrm = rrm;
        this.dispatcher = dispatcher;
    }
    
    public void run() {

        try {
	        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
	        ObjectInputStream in = new ObjectInputStream( socket.getInputStream());
	       
	        Object object = null;
	        RequestMessage requestMessage = null;
	        
	        boolean trueBoolean = true;
	        while (trueBoolean) {
	        	try {
	        		object = in.readObject();
	        		requestMessage = (RequestMessage) object;
	        	} catch (ClassNotFoundException e) {
	        		e.printStackTrace();
	        	}

	        	ReplyMessage replyMessage = dispatcher.dispatchCall(requestMessage, rrm.retrieve(requestMessage.to));
	        	if (replyMessage == null) continue;

	        	out.writeObject(replyMessage);

	        }
	        socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /*private RequestMessage deserializeRequestMessage(BufferedReader in) {
    	RequestMessage message = null;

    	try {
    		ObjectInputStream objIn = new ObjectInputStream(in);
    		e = (Employee) objIn.readObject();
    		objIn.close();
    	} catch(IOException i)
    	{
    		i.printStackTrace();
    		return null;
    	} catch(ClassNotFoundException c)
    	{
    		System.out.println("Employee class not found");
    		c.printStackTrace();
    		return null;
    	}
    }*/
}
