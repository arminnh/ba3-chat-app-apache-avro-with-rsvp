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
	        ReplyMessage replyMessage = null;
	        
	        boolean trueBoolean = true;
	        //while (trueBoolean) { //misschien timer bij doen in geval dat client opeen niets meer stuurt
	        	replyMessage = null;
	        	try {
	        		object = in.readObject();
	        		requestMessage = (RequestMessage) object;
	        		//requestMessage = (RequestMessage) in.readObject();
	        		
	        		// if request message == "end connection" -> break of trueBool = false ofzo iets
	        		System.out.println(requestMessage.from + "\n" + 
				        				requestMessage.to + "\n" +  
				        				requestMessage.methodName + "\n" +  
				        				requestMessage.paramTypes[0] + "\n" +  
				        				requestMessage.paramValues[0].toString() + "\n\n");
	        		
	        		replyMessage = dispatcher.dispatchCall(requestMessage, rrm.retrieve(requestMessage.to));
	        		
	        	} catch (ClassNotFoundException e) {
	        		e.printStackTrace();
	        	}
	        	
	        	//if (replyMessage == null) continue;
	        	out.writeObject(replyMessage);

	        //}
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
