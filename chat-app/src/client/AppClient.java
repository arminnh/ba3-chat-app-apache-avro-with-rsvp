package client;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import asg.cliche.Command;
import asg.cliche.ShellFactory;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import server.AppServerInterface;
import server.ClientInfo;
import videotest.FrameGrabber;
import videotest.MainTest;

/*
 * - automatische registratie bij de server
 * - lijst opvragen van alle online gebruikers
 * - gebruiker kan publieke chatroom joinen
 * - maximaal 1 conversatie tegelijk
 * 
 * 	 TODO
 * - requests voor prive gesprekken sturen, accepteren, weigeren
 *   (in elke situatie: publieke chatroom, prive gesprek)
 * - video streaming
 */

public class AppClient implements AppClientInterface {
	private SaslSocketTransceiver transceiver = null;
	private AppServerInterface appServer = null;
	private int clientPort, serverPort;
	private String clientIP, serverIP;
	private CharSequence username;
	private server.ClientInfo privateChatClient = null;
	private boolean privateChatClientArrived = false;
	private boolean privateChat = false;
	MainTest test = new MainTest();
	JFrame frame = new JFrame();
	Graphics g = null;

	public AppClient(SaslSocketTransceiver t, AppServerInterface a, String clientIP, int clientPort) {
		this.transceiver = t;
		this.appServer = a;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
	}

	public boolean register(String username) throws AvroRemoteException {
		if (this.appServer.isNameAvailable((CharSequence) username)) {
			appServer.registerClient((CharSequence) username, (CharSequence) clientIP, clientPort);
			this.username = username;
			return true;
		}
		return false;
	}

	@Command
	public void exit() throws IOException {
		appServer.exitClient(this.username);
		transceiver.close();
	}

	@Command
	public void getListOfUsers() throws IOException {
		CharSequence list = appServer.getListOfClients();
		System.out.println(list);
	}

	@Override
	public int leftPrivateChat() throws AvroRemoteException {
		System.out.println(this.privateChatClient.username + " has left the private chat. Messages you write will now not be sent.");
		System.out.println("You can now choose to go to either the lobby or the public chatroom.");

		try {
			this.privateChatClient.transceiver.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.privateChatClient = null;

		return 0;
	}

	@Override
	public int setPrivateChatClientArrived(boolean privateChatClientArrived) throws AvroRemoteException {
		this.privateChatClientArrived = privateChatClientArrived;
		return 0;
	}

	@Override
	public boolean videoRequest() throws AvroRemoteException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		try {
			String input = br.readLine();
			if (input.equals("y") || input.equals("y")) {
				System.out.println("accepted video");

				this.setFrameAndGraphics(400, 300);
				frame.setBounds(0,  0,  400,  300);
				g = frame.getGraphics();
				
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public int receiveImage(ByteBuffer imgBytes) throws AvroRemoteException {
		try {
    		ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes.array());
			Image img = ImageIO.read(bis);
			
	    	g.drawImage(img, 0, 0, frame.getWidth(), frame.getHeight(), null);
	    	
	    	return 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private void setFrameAndGraphics(int x, int y) {
		JPanel contentPane = new JPanel(new BorderLayout());
		frame = new JFrame();
		frame.getContentPane().add(contentPane);
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(x, y);
		frame.setVisible(true);
		g = frame.getGraphics();
	}
	
	public int destroyFrame() throws AvroRemoteException {
		frame.setVisible(false);
		frame.dispose();
		
		return 0;
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
	
	public void joinChat(boolean privateChat) throws IOException {
		if (!privateChat) {
			System.out.println("You entered the public chatroom.");
		} else {
			System.out.println("You entered a private chatroom with " + this.privateChatClient.username + ". Wait for them to arrive.");
		}
		System.out.println("The commands are different here, type ?list to get the list of commands.");

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		String input = br.readLine();

		while (!input.equals("?leave") && !input.equals("?q") ) {
			if (input.equals("?list")) {
				System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
				System.out.println("To leave the chatroom:              ?leave or ?q");
			} else if (input.equals("?getListOfUsers") || input.equals("?glou")) {
				getListOfUsers();
			} else if (privateChat) {
				if (input.equals("?joinPrivateChat") || input.equals("?jprc")) {
					System.out.println("Left the public chatroom.\nJoined private chat with " + this.privateChatClient.username + ".");
					this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
					privateChat = true;
				} else if (input.equals("?video")) {
					this.privateChatClient.proxy.receiveMessage(this.username + " has requested to videochat. Type y/n to accept/decline");
					if (this.privateChatClient.proxy.videoRequest()) {
						this.sendVideo();
					}
				} else if (this.privateChatClient != null && this.privateChatClientArrived) {
					this.privateChatClient.proxy.receiveMessage(input);
				}
			} else {
				if (input.equals("?joinPublicChat") || input.equals("?jpc")) {
					System.out.println("Left the private chat.\nJoined the public chatroom.");
					privateChat = false;
				} else {
					this.appServer.sendMessage(this.username, input);
				}
			}
			input = br.readLine();
		}

		if (privateChat && this.privateChatClient != null) {
			this.privateChatClient.proxy.leftPrivateChat();
			this.privateChatClient.transceiver.close();
			this.privateChatClient = null;
		}

		this.privateChatClientArrived = false;
		this.appServer.setClientState(this.username, server.ClientStatus.LOBBY);
		System.out.println("Left " + (privateChat ? "private" : "public") + " chat.");
	}

	@Command
	public void joinPublicChat() throws IOException {
		this.appServer.setClientState(this.username, server.ClientStatus.PUBLIC);
		this.joinChat(false);
	}

	@Override
	public int receiveMessage(CharSequence message) throws AvroRemoteException {
		System.out.println(message);
		return 0;
	}

	@Command
	public void listMyRequests() throws AvroRemoteException {
		System.out.println(this.appServer.getMyRequests(this.username));
	}

	@Command
	public void sendRequest(String username) throws AvroRemoteException {
		this.appServer.sendRequest((CharSequence) this.username, (CharSequence) username);
	}

	@Override
	public int setPrivateChatClient(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		ipaddress = ipaddress.toString().subSequence(1, ipaddress.length());
		System.out.println("ip: " + ipaddress.toString() + ":" + port);
		server.ClientInfo pcc = new server.ClientInfo();
		pcc.username = username.toString();
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ipaddress.toString());
			pcc.address = new InetSocketAddress(addr, port);
			pcc.transceiver = new SaslSocketTransceiver(pcc.address);
			pcc.proxy = (AppClientInterface) SpecificRequestor.getClient(AppClientInterface.class, pcc.transceiver);
			this.privateChatClient = pcc;
		} catch (UnknownHostException e) {	//InetAddress.getByName
			e.printStackTrace();
		}catch (IOException e) {			//SaslSocketTransceiver and SpecificRequestor
			e.printStackTrace();
		}

		return 0;
	}

	@Command
	public void acceptRequest(String username) {
		this.respondRequest(username, true);
	}

	@Command
	public void declineRequest(String username) {
		this.respondRequest(username, false);
	}

	@Command
	public void cancelRequest(String username) throws AvroRemoteException {
		this.appServer.cancelRequest(this.username, username);
	}

	public void respondRequest(String username, boolean responseBool) {
		try {
			int response = this.appServer.requestResponse(this.username, (CharSequence) username, responseBool);

			if (responseBool && response == 2) {
				this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
				this.privateChatClient.proxy.receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!\n");

				this.joinChat(true);
			}
		} catch (AvroRemoteException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int receiveRequest(CharSequence request) throws AvroRemoteException {
		System.out.println(request.toString());
		return 0;
	}

	@Command
	public int startPrivateChat() throws AvroRemoteException {
		this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
		try {
			this.privateChatClientArrived = true;
			this.privateChatClient.proxy.setPrivateChatClientArrived(true);
			this.privateChatClient.proxy.receiveMessage(this.username.toString() + " has entered the private chat.");
			this.appServer.removeRequest(this.username, this.privateChatClient.username);
			this.joinChat(true); //true means private chat
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return 0;
	}

	public static void main(String[] argv) {
		//Take server's ipaddress and port from terminal arguments:
		//	ipaddress = argv[0];
		//	port = argv[1];
		//Get hosts ipadress by:
		//  http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
		//  -> Just ask the user for now
		String clientIP = "0.0.0.0";
		int port = 6789, clientPort = 2345;

		/*try {
			InetAddress IP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} */

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

		Scanner in = new Scanner(System.in);
		System.out.println("Enter the IP address of the server.");
		InetSocketAddress serverIP = new InetSocketAddress(in.nextLine(), port);

		System.out.println("Enter the IP address the server will need to connect to.");
		clientIP = in.nextLine();
		//InetSocketAddress serverIP = new InetSocketAddress("0.0.0.0", port);

		Server clientResponder = null;
		AppClient clientRequester = null;
		try {
			// Connect to the server to create the appServer proxy object.
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(serverIP);
			AppServerInterface appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);
			//System.out.println("transceiver connected: " + transceiver.isConnected());
			//System.out.println("transceiver.getRemoteName(): " + transceiver.getRemoteName());

			while (true) { // Try other clientPorts for the case where the port is already in use 
				//System.out.println("Trying to use port " + clientPort);
				try {
					// Create a responder so the server can invoke methods on this client.
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
				if (clientRequester.register(username)) {
					break;
				}
				System.out.println("That name is already taken, choose another one.");
			}
			
			clientRequester.register(username);
			/*clientRequester.privateChatClient = new ClientInfo();

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
			
			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");
			ShellFactory.createConsoleShell("chat-app", "", clientRequester).commandLoop();
			System.out.println("Quit program.");

			//clientResponder.join();
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

	@Override
	public int echo(int message) throws AvroRemoteException {
		return message;
	}
}

// Runtime.getRuntime().addShutdownHook