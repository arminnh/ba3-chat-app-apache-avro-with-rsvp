package client;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.Image;
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

import server.*;
import video.*;
import errorwriter.ErrorWriter;

public class AppClient implements AppClientInterface, Runnable {
	private CharSequence username;
	private ClientStatus status;
	private String clientIP, serverIP;
	private int clientPort, serverPort;
	private AppServerInterface appServer;
	
	private ClientInfo privateChatClient;
	private boolean privateChatClientArrived = false;

	public JFrame senderFrame = new JFrame();
	public JFrame receiverFrame = new JFrame();
	public Graphics senderG = senderFrame.getGraphics();
	public Graphics receiverG = receiverFrame.getGraphics();

	//======================================================================================

	public AppClient(AppServerInterface a, String clientIP, int clientPort) {
		this.appServer = a;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.status = ClientStatus.LOBBY;

		this.senderFrame = new JFrame();
		this.senderFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.senderFrame.setBounds(250-250, 250,  400,  300);
		this.senderFrame.setVisible(true);
		this.senderG = this.senderFrame.getGraphics();
		
		this.receiverFrame = new JFrame();
		this.receiverFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.receiverFrame.setBounds(700-250, 250,  400,  300);
		this.receiverFrame.setVisible(true);
		this.receiverG = this.receiverFrame.getGraphics();
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
	public int setPrivateChatClient(CharSequence username, CharSequence ipaddress, int port) {
		//ipaddress = ipaddress.toString().subSequence(1, ipaddress.length());
		System.out.println("SetPrivateChatClient: ip address for " + username.toString() + " = " + ipaddress.toString() + ":" + port);
		ClientInfo client = new ClientInfo(username, ipaddress, port);

		// if client has already disconnected, proxy.function will throw an AvroRemoteException
		try {
			client.proxy.echo(666);
			this.privateChatClient = client;
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
		Scanner in = new Scanner(System.in);

		try {
			String input = in.nextLine();
			// if user has accepted the request
			if (input.matches("(?i)(y|yy)")) {
				System.out.println("You have accepted the video request.");
				this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has accepted the video request.");

				// set up the frame for the receiving side
				//this.setFrameAndGraphics(500, 25, 400, 300, false);
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

			this.receiverG.drawImage(img, 0, 0, this.receiverFrame.getWidth(), this.receiverFrame.getHeight(), null);

			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return 8;
		}
	}

	public int hideFrame() throws AvroRemoteException {
		//this.receiverFrame.setVisible(false);

		return 0;
	}

	/*
	 * COMMANDS
	 */

	@Command
	public void getListOfUsers() throws IOException {
		CharSequence list = appServer.getListOfClients();
		System.out.println(list);
	}

	@Command
	public void joinPublicChat() throws AvroRemoteException {
		this.setStatus(ClientStatus.PUBLIC);

		System.out.println("You entered the public chatroom.");
		this.joinChat();
		System.out.println("You have left the public chat.");
	}

	@Command
	public void listMyRequests() throws AvroRemoteException {
		System.out.println(this.appServer.getMyRequests(this.username));
	}

	@Command
	public void sendRequest(String username) throws AvroRemoteException {
		int response = this.appServer.sendRequest((CharSequence) this.username, (CharSequence) username);
		
		if (response != 0)
			ErrorWriter.printError(response);
	}

	@Command
	public void cancelRequest(String username) throws AvroRemoteException {
		int response = this.appServer.cancelRequest(this.username, username);
		
		if (response != 0)
			ErrorWriter.printError(response);
	}

	@Command
	public void acceptRequest(String username) throws AvroRemoteException {
		int response =  this.appServer.requestResponse((CharSequence) username, this.username, true);

		// server.requestResponse returned success code
		if (response == 0) {
			this.setStatus(ClientStatus.PRIVATE);
			this.privateChatClient.proxy.receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!\n");

			System.out.println("You entered a private chatroom with " + this.privateChatClient.username + ". Wait for them to arrive.");
			this.joinPrivateChat();
		} else {
			ErrorWriter.printError(response);
		}
	}

	@Command
	public void declineRequest(String username) throws AvroRemoteException {
		int response = this.appServer.requestResponse((CharSequence) username, this.username, false);

		if (response == 0) {
			System.out.println("You have declined the request from" + username);
		} else {
			ErrorWriter.printError(response);
		}
	}

	@Command
	public void startPrivateChat() throws AvroRemoteException {
		if (!this.appServer.isRequestStatus(this.username, RequestStatus.ACCEPTED)) {
			ErrorWriter.printError(11);
			return;
		}
		
		this.setStatus(ClientStatus.PRIVATE);

		this.privateChatClientArrived = true;
		this.privateChatClient.proxy.setPrivateChatClientArrived(true);
		this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has entered the private chat.");
		this.appServer.removeRequest(this.username, this.privateChatClient.username);

		System.out.println("You entered a private chatroom with " + this.privateChatClient.username);
		this.joinPrivateChat();
	}

	/*
	 * OTHER METHODS
	 */

	public int register(String username) throws AvroRemoteException {
		if (this.appServer.isNameAvailable((CharSequence) username)) {
			appServer.registerClient((CharSequence) username, (CharSequence) clientIP, clientPort);
			this.username = username;
			return 0;
		}

		return 9;
	}

	private int setStatus(ClientStatus status) throws AvroRemoteException {
		this.appServer.setClientState(this.username, status);
		this.status = status;
		return 0;
	}

	// TEST SWITCHING BETWEEN CHAT MODES
	private int joinChat() throws AvroRemoteException {
		System.out.println("The commands are different here, type ?list to get the list of commands.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			String input = br.readLine().toLowerCase();

			while (!input.matches("(\\?)(leave|q)")) {
				// get a list of available commands
				if (input.matches("(\\?)list")) {
					System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
					System.out.println("To leave the chatroom:              ?leave or ?q");
					if (this.appServer.isRequestStatus(this.username, RequestStatus.ACCEPTED))
						System.out.println("To start the private chat:          ?joinPrivateChat or ?jpc");
					if (this.status == ClientStatus.PRIVATE) {
						System.out.println("To send a video request:            ?video");
						System.out.println("Tgo the the public chatroom:        ?joinPublicChat or ?jpc");
					}

					// get a list of online users
				} else if (input.matches("(\\?)(getlistofusers|glou)")) {
					this.getListOfUsers();

					// go to the private chat
				} else if (input.matches("(\\?)(joinprivatechat|jprc|jpc|spc)") && this.appServer.isRequestStatus(this.username, RequestStatus.ACCEPTED)) {
					System.out.println("Left the public chatroom.\nJoined private chat with " + this.privateChatClient.username + ".");
					this.setStatus(ClientStatus.PRIVATE);

					// private chat commands
				} else if (this.status == ClientStatus.PRIVATE) {
					// go to the public chat
					if (input.matches("(\\?)(joinpublicchat|jpc)")) {
						System.out.println("Left the private chat.\nJoined the public chatroom.");
						this.setStatus(ClientStatus.PUBLIC);
						//TODO: private chat cleanup
					}
					if (this.privateChatClientArrived) {
						// send a video request
						if (input.matches("(\\?)video")) {
							this.privateChatClient.proxy.receiveMessage(this.username + " has requested to videochat. Type y/n to accept/decline");
							if (this.privateChatClient.proxy.videoRequest()) {
								this.sendVideo();
							}

							// send a message to the private chat partner
						} else if (this.privateChatClient != null) {
							this.privateChatClient.proxy.receiveMessage(input);
						}
					}

					// send message to everyone in the public chat
				} else {
					this.appServer.sendMessage(this.username, input);
				}
				input = br.readLine();
			}
		} catch (IOException e) {
			this.setStatus(ClientStatus.LOBBY);
			e.printStackTrace();
			return 10;
		}

		// set state back to lobby after finished
		this.setStatus(ClientStatus.LOBBY);
		return 0;
	}

	private void joinPrivateChat() throws AvroRemoteException {
		this.joinChat();
		System.out.println("You have left the private chat.");

		if (this.privateChatClient != null) {
			try {
				if (this.privateChatClientArrived) {
					this.privateChatClient.proxy.leftPrivateChat();
				}
				this.privateChatClient.transceiver.close();
				this.privateChatClient = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.privateChatClientArrived = false;
	}

	private void sendVideo() throws AvroRemoteException {
		//this.setFrameAndGraphics(25, 25, 400, 300, true);
		VideoFileChooser fc = new VideoFileChooser();
		
		//VideoSender videoSender = new VideoSender(new File("SampleVideo_1080x720_20mb.mkv"), frame, g, this.privateChatClient.proxy);
		//VideoSender videoSender = new VideoSender(new File("small.ogv"), frame, this.g, this.privateChatClient.proxy);
		//VideoSender videoSender = new VideoSender(new File("sample_mpeg4.mp4"), frame, this.g, this.privateChatClient.proxy);
		//VideoSender videoSender = new VideoSender(new File("ArchitectVideo_512kb.mp4"), frame, this.g, this.privateChatClient.proxy);
		//VideoSender videoSender = new VideoSender(new File("ArchitectVideo_dvd.mpg"), this.frame, this.g, this.privateChatClient.proxy);
		VideoSender videoSender = new VideoSender(fc.getFile(), this.senderFrame, this.senderG, this.privateChatClient.proxy);
		Thread sender = new Thread(videoSender);
		sender.start();
	}

	private void setFrameAndGraphics(int x, int y, int width, int height, boolean sending) {
		//JPanel contentPane = new JPanel(new BorderLayout());

		/*JFrame frame = sending ? this.senderFrame : this.receiverFrame;
		Graphics g = sending ? this.senderG : this.receiverG;
		
		frame.getContentPane().add(contentPane);
		// if there is no frame on screen yet
		frame.setBounds(x,  y,  width,  height);
		frame.setVisible(true);
		g = frame.getGraphics();
		if (sending) {
			this.senderG = this.senderFrame.getGraphics();
		} else {
			this.receiverG = this.receiverFrame.getGraphics();
		}*/
		/*if (sending) {
			this.senderFrame.setVisible(true);
		} else {
			this.receiverFrame.setVisible(true);
		}*/
		


		/*if (sending) {
			this.senderFrame = new JFrame();
			this.senderFrame.getContentPane().add(contentPane);
			this.senderFrame.setBounds(x+width+50,  y+height+50,  width,  height);
			this.senderFrame.setVisible(true);
			this.senderG = this.senderFrame.getGraphics();
			
		} else {
			this.receiverFrame = new JFrame();
			this.receiverFrame.getContentPane().add(contentPane);
			this.receiverFrame.setBounds(x,  y,  width,  height);
			this.receiverFrame.setVisible(true);
			this.receiverG = this.receiverFrame.getGraphics();
		}*/
	}

	// function that will be ran periodically by a Timer
	public void run() {
		
	}

	public static void main(String[] argv) {
		String clientIP = "0.0.0.0", serverIP = "0.0.0.0";
		int serverPort = 6789, clientPort = 2345;
		Scanner in = new Scanner(System.in);

		// if IP's are given as command line arguments
		if (argv.length == 2) {
			clientIP = argv[0];
			serverIP = argv[1];
			System.out.println("Got clientIP=" + clientIP + " and serverIP=" + serverIP + " from command line argumets");
		} else {
			/*System.out.println("Enter the IP address of the server.");
			serverIP = in.nextLine();
			System.out.println("Enter the IP address the server will need to connect to.");
			clientIP = in.nextLine();
			System.out.println("Got clientIP=" + clientIP + " and serverIP=" + serverIP);*/
		}

		Server clientResponder = null;
		AppClient clientRequester = null;
		SaslSocketTransceiver transceiver = null;
		AppServerInterface appServer = null;

		// connect to the server to create the appServer proxy object.
		try {
			transceiver = new SaslSocketTransceiver(new InetSocketAddress(serverIP, serverPort));
			appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);

			// try multiple clientPorts in case the port is already in use 
			while (true) { 
				// create a clientResponder so the appServer can invoke methods on this client.
				try {
					clientRequester = new AppClient(appServer, clientIP, clientPort);
					clientResponder = new SaslSocketServer( new SpecificResponder(AppClientInterface.class, clientRequester), new InetSocketAddress(clientPort) );
					clientResponder.start();
					System.out.println("Listening on ip " + clientIP + " and port " + clientPort);
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

			// choose a username and register it with the server
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

			appServer.unregisterClient(username);
			clientResponder.close();
			in.close();
			transceiver.close();
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
