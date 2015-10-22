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
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			while(true) {
				try {
					RequestMessage requestMessage = (RequestMessage) in.readObject();
					
					System.out.println(/*requestMessage.from + "\n" + */
							requestMessage.to + "\n" +  
							requestMessage.methodName + "\n" +  
							requestMessage.paramTypes[0] + "\n" +  
							requestMessage.paramValues[0].toString() + "\n\n");
		
					ReplyMessage replyMessage = dispatcher.dispatchCall(requestMessage, rrm.retrieve(requestMessage.to));
		
					out.writeObject(replyMessage);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (EOFException e) {
					break;
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			System.out.println("Socket has disconnected.");
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
