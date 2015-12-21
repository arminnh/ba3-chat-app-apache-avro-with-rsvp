package client;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.*;
import java.awt.Graphics;
import java.awt.BorderLayout;

import java.io.*;
import java.util.Scanner;
import java.net.*;
import java.nio.ByteBuffer;

import asg.cliche.Command;
import asg.cliche.ShellFactory;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;

import server.ClientInfo;
import server.AppServerInterface;

public class AppClient implements AppClientInterface {
	//TODO: remove tranceiver
	private SaslSocketTransceiver transceiver = null;
	private AppServerInterface appServer = null;
	private int clientPort, serverPort;
	private String clientIP, serverIP;
	private CharSequence username;
	private ClientInfo privateChatClient = null;
	private boolean privateChatClientArrived = false;
	private boolean privateChat = false;
	JFrame frame = new JFrame();
	Graphics g = null;

	//======================================================================================

	public AppClient(SaslSocketTransceiver t, AppServerInterface a, String clientIP, int clientPort) {
		this.transceiver = t;
		this.appServer = a;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
	}

	/*
	 *  INVOKABLE METHODS
	 */

	@Override
	public int echo(int message) throws AvroRemoteException {
		return message;
	}

	@Override
	public int receiveMessage(CharSequence message) throws AvroRemoteException {
		System.out.println(message);
		return 0;
	}

	@Override
	public int receiveRequest(CharSequence request) throws AvroRemoteException {
		System.out.println(request.toString());
		return 0;
	}

	@Override
	//TODO: test if still correct
	public int setPrivateChatClient(CharSequence username, CharSequence ipaddress, int port) {
		ipaddress = ipaddress.toString().subSequence(1, ipaddress.length());
		System.out.println("SetPrivateChatClient: ip adrress for " + username.toString() + " = " + ipaddress.toString() + ":" + port);
		ClientInfo client = new ClientInfo(username, ipaddress, port);

		// if client has already disconnected, proxy.function will throw an AvroRemoteException
		try {
			client.proxy.echo(666);
			return 0;
		} catch (AvroRemoteException e) {
			return 1;
		}
	}

	@Override
	public int setPrivateChatClientArrived(boolean privateChatClientArrived) throws AvroRemoteException {
		this.privateChatClientArrived = privateChatClientArrived;
		return 0;
	}

	@Override
	public int leftPrivateChat() throws AvroRemoteException {
		System.out.println(this.privateChatClient.username + " has left the private chat. Messages you write will now not be sent.");
		System.out.println("You can now choose to go to either the lobby or the public chatroom.");
		this.privateChatClientArrived = false;
		
		return 0;
	}

	@Override
	public boolean videoRequest() throws AvroRemoteException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		//Scanner in = new Scanner(System.in);

		try {
			String input = br.readLine();
			// if user has accepted the request
			if (input.equals("y") || input.equals("Y") || input.equals("yy") || input.equals("YY")) {
				System.out.println("You have accepted the video request.");
				this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has accepted the video request.");

				// set up the frame for the receiving side
				this.setFrameAndGraphics(400, 300);
				frame.setBounds(0,  0,  400,  300);
				g = frame.getGraphics();

				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("You have declined the video request.");
		this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has declined the video request.");
		return false;
	}

	@Override
	public int receiveImage(ByteBuffer imgBytes) throws AvroRemoteException {
		try {
			// convert the input ByteBuffer to an Image using ByteArrayInputStream 
			ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes.array());
			Image img = ImageIO.read(bis);

			g.drawImage(img, 0, 0, frame.getWidth(), frame.getHeight(), null);

			return 1;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public int destroyFrame() throws AvroRemoteException {
		frame.setVisible(false);
		frame.dispose();

		return 0;
	}

	/*
	 * METHODS WHICH USE INVOKABLE METHODS
	 */

	public int register(String username) throws AvroRemoteException {
		// if name was not already taken
		if (this.appServer.isNameAvailable((CharSequence) username)) {
			appServer.registerClient((CharSequence) username, (CharSequence) clientIP, clientPort);
			this.username = username;
			return 0;
		}

		return 1;
	}

	@Command
	public void listMyRequests() throws AvroRemoteException {
		System.out.println(this.appServer.getMyRequests(this.username));
	}

	@Command
	public void sendRequest(String username) throws AvroRemoteException {
		this.appServer.sendRequest((CharSequence) this.username, (CharSequence) username);
	}

	@Command
	public void cancelRequest(String username) throws AvroRemoteException {
		this.appServer.cancelRequest(this.username, username);
	}

	@Command
	public void acceptRequest(String username) throws AvroRemoteException {
		int response =  this.appServer.requestResponse((CharSequence) username, this.username, true);
		
		// server.requestResponse returned success code
		if (response == 0) {
			this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
			this.privateChatClient.proxy.receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!\n");

			this.joinPrivateChat();
		} else {
			System.out.println("Something went wrong, error code: " + response);
		}
	}

	@Command
	public void declineRequest(String username) throws AvroRemoteException {
		int response = this.appServer.requestResponse((CharSequence) username, this.username, false);
		
		if (response == 0) {
			System.out.println("You have declined the request from" + username);
		} else {
			System.out.println("You do not have any open requests from" + username);
		}
	}

	@Command
	public void startPrivateChat() throws AvroRemoteException {
		this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
		
		this.privateChatClientArrived = true;
		this.privateChatClient.proxy.setPrivateChatClientArrived(true);
		this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has entered the private chat.");
		this.appServer.removeRequest(this.username, this.privateChatClient.username);
		
		this.joinPrivateChat();
	}

	private void sendVideo() throws AvroRemoteException {
		this.setFrameAndGraphics(400, 300);

		//		VideoSender videoSender = new VideoSender(new File("SampleVideo_1080x720_20mb.mkv"), frame, this.privateChatClient.proxy);
		//		VideoSender videoSender = new VideoSender(new File("small.ogv"), frame, this.privateChatClient.proxy);
		//		VideoSender videoSender = new VideoSender(new File("sample_mpeg4.mp4"), frame, this.privateChatClient.proxy);
		//		VideoSender videoSender = new VideoSender(new File("ArchitectVideo_512kb.mp4"), frame, this.privateChatClient.proxy);
		VideoSender videoSender = new VideoSender(new File("ArchitectVideo_dvd.mpg"), frame, this.privateChatClient.proxy);
		//		VideoSender videoSender = new VideoSender(new File(""), frame, this.privateChatClient.proxy);
		Thread sender = new Thread(videoSender);
		sender.start();
	}

	/*
	 * OTHER METHODS
	 */

	@Command
	public void exit() throws IOException {
		appServer.unregisterClient(this.username);
		transceiver.close();
	}

	@Command
	public void getListOfUsers() throws IOException {
		CharSequence list = appServer.getListOfClients();
		System.out.println(list);
	}

	// TEST IF WORKS CORRECTLY
	public int joinChat(boolean privateChat) throws AvroRemoteException {
		System.out.println("The commands are different here, type ?list to get the list of commands.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			String input = br.readLine();

			while (!input.matches("(?i)?leave|?q")) {
				// get a list of available commands
				if (!input.matches("(?i)?list")) {
					System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
					System.out.println("To leave the chatroom:              ?leave or ?q");

				// get a list of online users
				} else if (input.matches("(?i)?getListOfUsers|?glou")) {
					this.getListOfUsers();

				// go to the private chat
				} else  if (input.matches("(?i)?joinPrivateChat|jprc|jpc|spc")) {
					System.out.println("Left the public chatroom.\nJoined private chat with " + this.privateChatClient.username + ".");
					this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
					privateChat = true;

				// private chat commands
				} else if (privateChat) {
					// go to the public chat
					if (input.matches("(?i)?joinPublicChat|jpc")) {
						System.out.println("Left the private chat.\nJoined the public chatroom.");
						privateChat = false;

					// send a video request
					} else if (input.matches("(?i)?video")) {
						this.privateChatClient.proxy.receiveMessage(this.username + " has requested to videochat. Type y/n to accept/decline");
						if (this.privateChatClient.proxy.videoRequest()) {
							this.sendVideo();
						}

					// send a message to the private chat partner
					} else if (this.privateChatClient != null && this.privateChatClientArrived) {
						this.privateChatClient.proxy.receiveMessage(input);
					}

				// send message to everyone in the public chat
				} else {
					this.appServer.sendMessage(this.username, input);
				}
				input = br.readLine();
			}
			br.close();
			
			// set state back to lobby after finished
			this.appServer.setClientState(this.username, server.ClientStatus.LOBBY);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		return 0;
	}
	

	@Command
	public void joinPublicChat() throws AvroRemoteException {
		this.appServer.setClientState(this.username, server.ClientStatus.PUBLIC);
		
		System.out.println("You entered the public chatroom.");
		this.joinChat(false); // true will use public chat mode
		System.out.println("You have left the public chat.");
	}
	

	@Command
	public void joinPrivateChat() throws AvroRemoteException {
		System.out.println("You entered a private chatroom with " + this.privateChatClient.username + ". Wait for them to arrive.");
		this.joinChat(true); // true will use private chat mode
		System.out.println("You have left the private chat.");

		//TODO: finish
		if (this.privateChatClient != null) {
			try {
				this.privateChatClient.proxy.leftPrivateChat();
				this.privateChatClient.transceiver.close();
				this.privateChatClient = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.privateChatClientArrived = false;
	}

	@Command
	private void setFrameAndGraphics(int x, int y) {
		JPanel contentPane = new JPanel(new BorderLayout());
		
		frame = new JFrame();
		frame.getContentPane().add(contentPane);
		frame.setSize(x, y);
		frame.setVisible(true);
		
		g = frame.getGraphics();
	}

	public static void main(String[] argv) {
		//TODO: Take server's ipaddress and port from terminal arguments:
		//	if argv.length ... else user input
		String clientIP = "0.0.0.0";
		int serverPort = 6789, clientPort = 2345;
		
		Scanner in = new Scanner(System.in);
		System.out.println("Enter the IP address of the server.");
		InetSocketAddress serverIP = new InetSocketAddress(in.nextLine(), serverPort);
		//serverIP = new InetSocketAddress("0.0.0.0", port);  // serverIP = new InetAddress.getLocalHost()

		System.out.println("Enter the IP address the server will need to connect to.");
		clientIP = in.nextLine();

		Server clientResponder = null;
		AppClient clientRequester = null;

		// Connect to the server to create the appServer proxy object.
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(serverIP);
			AppServerInterface appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);
			
			// Try multiple clientPorts in case the port is already in use 
			while (true) { 
				// Create a clientResponder so the appServer can invoke methods on this client.
				try {
					clientRequester = new AppClient(transceiver, appServer, clientIP, clientPort);
					clientResponder = new SaslSocketServer( new SpecificResponder(AppClientInterface.class, clientRequester), new InetSocketAddress(clientPort) );
					clientResponder.start();
					System.out.println("Listening on ip:port" + clientIP + ":" + clientPort);
					break;
				} catch (java.net.BindException e) {
					if (clientPort < 65535) {
						clientPort++;
					} else {
						System.err.println("Failed to find open port, quitting program.");
						System.exit(1);
					}
				}
			}

			String username;
			System.out.println("Enter your username.");
			while (true) {
				username = in.nextLine();
				if (clientRequester.register(username) == 0) {
					break;
				}
				System.out.println("That name is already taken, choose another one.");
			}

			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");
			ShellFactory.createConsoleShell("chat-app", "", clientRequester).commandLoop();
			System.out.println("Quit program.");

			clientResponder.close();
			in.close();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error connecting to server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

	}

}


//Get hosts ipadress by:
//  http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
//  -> Just ask the user for now




/*try {
	Enumeration e = NetworkInterface.getNetworkInterfaces();
	while(e.hasMoreElements()) {
	    NetworkInterface n = (NetworkInterface) e.nextElement();
	    Enumeration ee = n.getInetAddresses();
	    while (ee.hasMoreElements()) {
	        InetAddress i = (InetAddress) ee.nextElement();
	        System.out.println(i.getHostAddress());
	        System.out.println("i.isAnyLocalAddress(): " + i.isAnyLocalAddress());
	        System.out.println("i.isLinkLocalAddress(): " + i.isLinkLocalAddress());
	        System.out.println("i.isLoopbackAddress(): " + i.isLoopbackAddress());
			System.out.println();
	    }
	}
} catch (SocketException e1) {
	e1.printStackTrace();
}*/




//System.out.println("transceiver connected: " + transceiver.isConnected());
//System.out.println("transceiver.getRemoteName(): " + transceiver.getRemoteName());


/*
clientRequester.register(username);
clientRequester.privateChatClient = new ClientInfo();

if (username.equals("a")) {
	clientPort = 2346;
	System.out.println("clientport =" + clientPort);
}
else clientPort = 2345;

while (true) {  
	try {
		clientRequester.setPrivateChatClient("b", "00.0.0.0", clientPort);
		break;
	} catch (Exception e) {
		if (clientPort < 65535) {
			clientPort++;
		} else {
			System.err.println("Failed to find open port, quitting program.");
			System.exit(1);
		}
	}
}
clientRequester.startPrivateChat();*/
