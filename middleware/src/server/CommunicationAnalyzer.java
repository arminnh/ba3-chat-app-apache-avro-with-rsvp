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
	        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
	        
	        String inputLine, outputLine;
	        while ((inputLine = in.readLine()) != null) {
	        	RequestMessage requestMessage = this.deserializeRequestMessage(in);
	        	ReplyMessage replyMessage = dispatcher.dispatchCall(requestMessage, rrm.retrieve(requestMessage.to));
	            outputLine = out.println(outputLine);
	            if (outputLine.equals("Bye"))
	                break;
	        }
	        socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private RequestMessage deserializeRequestMessage(BufferedReader in) {
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
    }
}
