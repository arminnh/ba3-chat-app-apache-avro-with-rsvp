package client;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Graphics;
import java.awt.BorderLayout;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
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
import rsvp.RSVP;

public class AppClient extends TimerTask implements AppClientInterface {
	private CharSequence username;
	private ClientStatus status;
	private String clientIP, serverIP;
	private int clientPort, serverPort, reconnectAttempts;
	private AppServerInterface appServer;

	private ClientInfo privateChatClient;
	private boolean privateChatClientArrived, videoRequestPending, videoRequestAccepted, connectedToServer;

	private JFrame senderFrame = new JFrame();
	private JFrame receiverFrame = new JFrame();
	private Graphics receiverG;
	
	private RSVP rsvp;

	// ======================================================================================

	public AppClient(AppServerInterface a, String clientIP, int clientPort, String serverIP, int serverPort) {
		this.appServer = a;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.status = ClientStatus.LOBBY;
		
		try {
			this.rsvp = new RSVP(InetAddress.getByName("localhost"), 10000, this.clientIP, this.clientPort);
		} catch (Exception e) {
			this.rsvp = null;
			e.printStackTrace();
		}
	}

	/*
	 * INVOKABLE METHODS
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
	public int setPrivateChatClient(CharSequence username, CharSequence ipaddress, int port) {
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
		String name = this.privateChatClient.username != null ? this.privateChatClient.username : "Your chat buddy";
		System.out.println("\n > " + name + " has left the private chat. Messages you write will now not be sent.");
		System.out.println(" > You can now choose to go to either the lobby or the public chatroom.");
		this.privateChatClientArrived = false;

		return 0;
	}

	@Override
	public int videoRequest() throws AvroRemoteException {
		System.out.println("\n > " + this.privateChatClient.username + " has requested to videochat. Use the ?acceptVideo or ?declineVideo commands to accept or decline.");
		this.videoRequestPending = true;

		return 0;
	}

	@Override
	public int videoRequestAccepted() throws AvroRemoteException {
		System.out.println("\n > " + this.privateChatClient.username + " has accepted your video request. Use the ?sendVideo command to start.");
		this.videoRequestAccepted = true;
		return 0;
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

	@Override
	public int setFrameVisible(boolean receiving, boolean visible) throws AvroRemoteException {
		if (receiving) {
			this.receiverFrame.setVisible(visible);
			if (visible)
				this.receiverG = this.receiverFrame.getGraphics();
		} else {
			this.senderFrame.setVisible(visible);
			if (visible)
				this.receiverG = this.receiverFrame.getGraphics();
		}

		return 0;
	}
	
	@Override
	public boolean isFrameVisible(boolean receiving) throws AvroRemoteException {
		if (receiving) {
			return this.receiverFrame.isVisible();
		} else {
			return this.senderFrame.isVisible();
		}
	}

	@Override
	public int shutdownPrivateChat(boolean first) {
		this.privateChatClientArrived = false;
		this.videoRequestPending = false;
		this.videoRequestAccepted = false;
		
		this.senderFrame.setVisible(false);
		this.receiverFrame.setVisible(false);

		//TODO: break down RSVP, tear messages ?
		if (this.rsvp != null && this.privateChatClient != null) {
			this.rsvp.tearPath(this.clientIP, this.clientPort, this.privateChatClient.clientIP.toString(), this.privateChatClient.clientPort);
			this.rsvp.tearResv(this.privateChatClient.clientIP.toString(), this.privateChatClient.clientPort, this.clientIP, this.clientPort);
		}
		
		if (this.privateChatClient != null) {
			this.privateChatClient.shutdown(first);
			this.privateChatClient = null;
		}
		
		return 0;
	}

	/*
	 * COMMANDS
	 */

	@Command
	public void getListOfUsers() throws IOException {
		if (!this.isConnectedToServer())
			return;

		CharSequence list = appServer.getListOfClients();
		System.out.println(list);
	}

	@Command
	public void joinPublicChat() throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		this.setStatus(ClientStatus.PUBLIC);

		System.out.println("\n > " + "You entered the public chatroom.");
		int response = this.joinChat();
		if (response != 0)
			ErrorWriter.printError(response);
	}

	@Command
	public void listMyRequests() throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		System.out.println(this.appServer.getMyRequests(this.username));
	}

	@Command
	public void sendRequest(String username) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		if (this.username.equals(username)) {
			System.err.println("\n > " + "You cannot send a request to yourself.");
			return;
		}
		
		if (this.appServer.isRequestStatus((CharSequence) username, this.username, RequestStatus.PENDING)) {
			this.acceptRequest(username, false);
			return;
		}
		
		if (this.privateChatClient != null && this.privateChatClient.username.equals(username)) {
			System.err.println("You are already in a private chat session with that user.");
			return;
		}
		
		int response = this.appServer.sendRequest((CharSequence) this.username, (CharSequence) username);

		if (response == 0) 
			System.out.println("\n > " + "You have sent a private chat request to " + username + ".");
		else
			ErrorWriter.printError(response);
	}

	@Command
	public void cancelRequest(String username) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		int response = this.appServer.cancelRequest(this.username, username);

		if (response != 0)
			ErrorWriter.printError(response);
	}

	@Command
	public void acceptRequest(String username) throws AvroRemoteException {
		this.acceptRequest(username, false);
	}
	
	@Command
	public void declineRequest(String username) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		int response = this.appServer.requestResponse((CharSequence) username, this.username, false);

		if (response == 0) {
			System.out.println("\n > You have declined the request from " + username + ".");
		} else {
			ErrorWriter.printError(response);
		}
	}

	@Command
	public void startPrivateChat() throws AvroRemoteException {
		this.startPrivateChat(false);
	}
	
	/*
	 * CHAT RELATED METHODS
	 */
	
	private int joinChat() throws AvroRemoteException {
		System.out.println(" > The commands are different here, type ?list to get the list of commands.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			String input = br.readLine().toLowerCase();

			while (!input.matches("(\\?)(leave|q)")) {
				chatCommands(input);
				input = br.readLine().toLowerCase();
			}
			
			if (this.status == ClientStatus.PRIVATE)
				this.shutdownPrivateChat(this.privateChatClient != null);
			
			System.out.println("\n > You have left the chatroom.");
		} catch (Exception e) {
			e.printStackTrace();
			this.setStatus(ClientStatus.LOBBY);
			return 10;
		}

		if (!this.connectedToServer) {
			System.err.println("\n > Could not connect to server. Restart the app to try again later.");
			System.exit(1);
		}
		// set state back to lobby after finished
		this.setStatus(ClientStatus.LOBBY);
		return 0;
	}

	private void chatCommands(String input) throws IOException {
		// check for all the global commands first, then public/private chat
		if (input.matches("(\\?)(list|l)")) {
			chatCommandsList();

		} else if (input.matches("(\\?)(getlistofusers|glou)")) {
			getListOfUsers();

		} else if (input.matches("(\\?)(listmyrequests|lmr)")) {
			listMyRequests();

		} else if (input.matches("(\\?)(sendrequest|sr)\\s+[^\\s]+")) {
			sendRequest(input.split("\\s+")[1]);

		} else if (input.matches("(\\?)(cancelrequest|cr)\\s+[^\\s]+")) {
			cancelRequest(input.split("\\s+")[1]);

		} else if (input.matches("(\\?)(declinerequest|dr)\\s+[^\\s]+")) {
			declineRequest(input.split("\\s+")[1]);

		} else if (status == ClientStatus.PUBLIC) {
			publicChatCommands(input);

		} else if (status == ClientStatus.PRIVATE) {
			privateChatCommands(input);
		}
	}

	private void chatCommandsList() throws AvroRemoteException {
		// global commands
		System.out.println("=================             GLOBAL COMMANDS             ==================\n"
					     + "Print this list:                 ?list,  ?l\n"
					     + "Leave the chatroom:              ?leave, ?q\n");

		if (this.connectedToServer) {
			System.out.println("Get the list of connected users: ?getListOfUsers,   ?glou\n"
						     + "List your requests:              ?listMyRequests,   ?lmr\n"
						     + "Send a request to X:             ?sendRequest X,    ?sr X\n"
						     + "Cancel a request to X:           ?cancelRequest X,  ?cr X\n"
						     + "Decline a request from X:        ?declineRequest X, ?dr X\n");

			// only public chat commands
			if (status == ClientStatus.PUBLIC) {
				System.out.println("\n=================          PUBLIC CHAT COMMANDS          ===================\n");
				
				if (appServer.isRequestStatusTo(this.username, RequestStatus.PENDING))
					System.out.println("Accept a request from X:         ?acceptRequest X,  ?ar X");

				if (appServer.isRequestStatusFrom(username, RequestStatus.ACCEPTED))
					System.out.println("To start the private chat:       ?joinPrivateChat, ?jpc, ?startPrivateChat, ?spc");
			}
		}

		// only private chat commands
		if (status == ClientStatus.PRIVATE) {
			System.out.println("\n=================          PRIVATE CHAT COMMANDS          ==================\n"
						     + "Go to the the public chatroom:   ?joinPublicChat, ?jpc\n"
						     + "Send a video request:            ?videoRequest,   ?vr, ?sendVideoRequest, ?svr");

			if (videoRequestAccepted)
				System.out.println("Send a video:                    ?sendVideo,    ?sv");

			if (videoRequestPending) {
				System.out.println("Accept a video request:          ?acceptVideo,  ?av\n"
							     + "Decline a video request:         ?declineVideo, ?dv");
			}
		}
	}

	private void publicChatCommands(String input) throws IOException {
		if (input.matches("(\\?)(acceptrequest|ar)\\s+[^\\s]+")) {
			acceptRequest(input.split("\\s+")[1], true);

			// go to the private chat if you have an open request that was accepted
		} else if (input.matches("(\\?)(joinprivatechat|jpc|startprivatechat|spc)") && appServer.isRequestStatusFrom(username, RequestStatus.ACCEPTED)) {
			System.out.println("\n > Left the public chatroom.");
			this.startPrivateChat(true);
			this.setStatus(ClientStatus.PRIVATE);

			// if no command was detected, send input to everyone in public chat mode
		} else if (this.isConnectedToServer()) {
			appServer.sendMessage(username, input);
		}
	}

	private void privateChatCommands(String input) throws AvroRemoteException {
		if (input.matches("(\\?)(joinpublicchat|jpc)")) {
			System.out.println("\n > Left the private chat.\n > Joined the public chatroom.");
			this.shutdownPrivateChat(this.privateChatClient != null);
			this.setStatus(ClientStatus.PUBLIC);
		}

		if (privateChatClientArrived && this.privateChatClient != null) {
			if (input.matches("(\\?)(sendvideorequest|videorequest|svr|vr)")) {
				if (!this.senderFrame.isVisible()) {
					System.out.println("\n > You have sent a video request.");
					//TODO: send path message = request for QoS reservation
					if (this.rsvp != null)
						this.rsvp.requestQoS(this.clientIP, this.clientPort, this.privateChatClient.clientIP.toString(), this.privateChatClient.clientPort);
					
					this.privateChatClient.proxy.videoRequest();
				} else {
					System.err.println("\n > You are already streaming a video.");
				}

			} else if (input.matches("(\\?)(sendvideo|sv)") && videoRequestAccepted) {
				sendVideo();

			} else if (videoRequestPending && input.matches("(\\?)(acceptvideo|av)")) {
				acceptVideoRequest();
			} else if (videoRequestPending && input.matches("(\\?)(declinevideo|dv)")) {
				declineVideoRequest();

				// if no command was detected, send input to the private chat partner
			} else {
				Date dNow = new Date( );
				SimpleDateFormat dFormat = new SimpleDateFormat ("hh:mm");
				String time = dFormat.format(dNow);
				this.privateChatClient.proxy.receiveMessage(time + ": " + input);
			}
		}
	}
	
	private void startPrivateChat(boolean isInChatMode) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		if (!this.appServer.isRequestStatusFrom(this.username, RequestStatus.ACCEPTED)) {
			ErrorWriter.printError(11);
			return;
		}

		this.setStatus(ClientStatus.PRIVATE);

		this.privateChatClientArrived = true;
		this.privateChatClient.proxy.setPrivateChatClientArrived(true);
		this.privateChatClient.proxy.receiveMessage("\n > " + this.username.toString() + " has entered the private chat.");
		this.appServer.removeRequest(this.username, this.privateChatClient.username);

		System.out.println("\n > You entered a private chatroom with " + this.privateChatClient.username);
		if (!isInChatMode)
			this.joinPrivateChat();
	}

	private void declineVideoRequest() throws AvroRemoteException {
		System.out.println("You have declined the video request.");
		this.videoRequestPending = false;
		this.privateChatClient.proxy.receiveMessage("\n > " + this.username.toString() + " has declined the video request.");
	}

	private void acceptVideoRequest() throws AvroRemoteException {
		System.out.println("\n > You have accepted the video request.");
		this.videoRequestPending = false;
		this.privateChatClient.proxy.videoRequestAccepted();
		
		//TODO: send resv message = accept QoS reservation
		if (this.rsvp != null) {
			this.rsvp.confirmQoS(this.privateChatClient.clientIP.toString(), this.privateChatClient.clientPort, this.clientIP, this.clientPort);
		}
	}

	private void sendVideo() throws AvroRemoteException {
		VideoFileChooser fc = new VideoFileChooser();
		File video = fc.getFile();
		
		try {
			video.canRead();
		} catch (NullPointerException e) { 
			this.privateChatClient.proxy.receiveMessage(" > " + this.username.toString() + " has cancelled the video request.");
			System.out.println(" > You cancelled the video request.");
			this.videoRequestAccepted = false;
			return;
		}
		this.senderFrame.setVisible(true);
		this.privateChatClient.proxy.setFrameVisible(true, true);
		VideoSender videoSender = new VideoSender(video, this.senderFrame, this.privateChatClient.proxy);
		Thread sender = new Thread(videoSender);
		sender.start();

		this.videoRequestAccepted = false;
	}

	private void joinPrivateChat() throws AvroRemoteException {
		int response = this.joinChat();

		if (response != 0)
			ErrorWriter.printError(response);

		this.shutdownPrivateChat(this.privateChatClient != null);
	}

	/*
	 * OTHER METHODS
	 */

	private int register(String username) throws AvroRemoteException {
		username = username.replaceAll("\\s+","");
		if (this.appServer.isNameAvailable((CharSequence) username)) {
			this.appServer.registerClient((CharSequence) username, (CharSequence) clientIP, clientPort);
			this.username = username;
			this.connectedToServer = true;
			return 0;
		}

		return 9;
	}

	public String registerUser(Scanner in) throws AvroRemoteException {
		// choose a username and register it with the server
		String username;
		System.out.println(" > Enter your username.");
		while (true) {
			username = in.nextLine();
			if (this.register(username) == 0) {
				break;
			}
			System.out.println("\n > That name is not available, choose another one.");
		}
		return username;
	}

	public void initJFrames(int x, int y, int width, int height) {
		this.senderFrame = new JFrame();
		this.senderFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.senderFrame.setBounds(x, y, width, height);
		this.senderFrame.setTitle("Sending Video");

		this.senderFrame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
            	try {
                	if (privateChatClient != null) {
                		if (privateChatClient.proxy.isFrameVisible(true)) {
                			privateChatClient.proxy.receiveMessage(" > " + username + " stopped sending video.");
                			System.out.println(" > You stopped sending the video.");
                		}
                		privateChatClient.proxy.setFrameVisible(true, false);
                	}
            	} catch (Exception e) {
            		
            	}
            }
        } );

		this.receiverFrame = new JFrame();
		this.receiverFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.receiverFrame.setBounds(x + width + 50, y, width, height);
		this.receiverFrame.setTitle("Receiving Video");

		this.receiverFrame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
            	try {
                	if (privateChatClient != null) {
                		if (privateChatClient.proxy.isFrameVisible(false)) {
                			privateChatClient.proxy.receiveMessage(" > " + username + " stopped receiving video.");
                			System.out.println(" > You stopped receiving the video.");
                		}
                		privateChatClient.proxy.setFrameVisible(false, false);
                	}
            	} catch (Exception e) {
            		
            	}
            }
        } );
	}
	
	private int setStatus(ClientStatus status) {
		try {
			this.appServer.setClientState(this.username, status);
		} catch (AvroRemoteException e) {
			System.err.println("Not connected to server right now.");
		}
		this.status = status;
		return 0;
	}
	
	private void acceptRequest(String username, boolean isInChatMode) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;
		
		// if somebody else already accepted a request from that user, dont allow this accept
		if (this.appServer.isRequestStatusFrom((CharSequence) username, RequestStatus.ACCEPTED)) {
			System.err.println("Somebody else already accepted a request from " + username + ", try again later.");
			return;
		}

		int response = this.appServer.requestResponse((CharSequence) username, this.username, true);

		// server.requestResponse returned success code
		if (response == 0) {
			this.setStatus(ClientStatus.PRIVATE);
			this.privateChatClient.proxy .receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!");

			System.out.println("\n > " + "You started a private chatroom with " + this.privateChatClient.username + ". Wait for them to arrive.");
			if (!isInChatMode)
				this.joinPrivateChat();
		} else {
			ErrorWriter.printError(response);
		}
	}

	private boolean isConnectedToServer() {
		if (!this.connectedToServer) {
			System.err.println(" > Not connected to server right now.");
			return false;
		}

		return true;
	}

	// function that will be ran periodically by a Timer
	public void run() {
		// if disconnected from server, proxy.function will throw an AvroRemoteException
		try {
			this.appServer.echo(666);
		} catch (AvroRemoteException e) {
			// try to reconnect up to 10 times
			if (this.reconnectAttempts <= 10) {
				System.err.println("\n > Disconnected from server. Trying to reconnect... " + this.reconnectAttempts);
				this.reconnectAttempts++;

				try {
					SaslSocketTransceiver transceiver = new SaslSocketTransceiver( new InetSocketAddress(this.serverIP, this.serverPort));
					this.appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);
					System.out.println("\n > Reconnected to server.");
					this.reconnectAttempts = 0;
					this.connectedToServer = true;
					this.register(this.username.toString());
				} catch (IOException e1) {
					this.connectedToServer = false;
				}

			} else {
				// if chatting with someone
				if (this.privateChatClient != null) {
					if (this.reconnectAttempts == 11) {
						System.err.println("\n > Could not connect to server. Restart the app to try again later.");
						System.out.println(" > You can still finish your chat session.");
						this.reconnectAttempts++;
					}
				} else {
					System.err.println("\n > Could not connect to server. Restart the app to try again later.");
					System.exit(1);
				}
			}
		}

		// if disconnected from private chat, proxy.function will throw an AvroRemoteException
		try {
			if (this.privateChatClient != null) {
				this.privateChatClient.proxy.echo(666);
			}
		} catch (AvroRemoteException e) {
			System.out.println("\n > Disconnected from private chat. You can now choose to go to either the lobby or the public chatroom. ");
			this.shutdownPrivateChat(this.privateChatClient != null);

		}
	}

	private static void printWelcome() {
		System.out.println(" _    _ _____ _     _____ ________  ________     _____ _____     _____ _   _  ___ _____       ___ ____________ \n"
						+ "| |  | |  ___| |   /  __ \\  _  |  \\/  |  ___|   |_   _|  _  |   /  __ \\ | | |/ _ \\_   _|     / _ \\| ___ \\ ___ \\\n"
						+ "| |  | | |__ | |   | /  \\/ | | | .  . | |__       | | | | | |   | /  \\/ |_| / /_\\ \\| |______/ /_\\ \\ |_/ / |_/ /\n"
						+ "| |/\\| |  __|| |   | |   | | | | |\\/| |  __|      | | | | | |   | |   |  _  |  _  || |______|  _  |  __/|  __/ \n"
						+ "\\  /\\  / |___| |___| \\__/\\ \\_/ / |  | | |___      | | \\ \\_/ /   | \\__/\\ | | | | | || |      | | | | |   | |    \n"
						+ " \\/  \\/\\____/\\_____/\\____/\\___/\\_|  |_|____/      \\_/  \\___/     \\____|_| |_|_| |_/\\_/      \\_| |_|_|   \\_|    ");

		System.out.println("\n > Welcome to Chat App, type ?list to get a list of available commands.");
	}

	/*
	 * MAIN METHOD
	 */
	
	public static void main(String[] argv) {
		String clientIP = "0.0.0.0", serverIP = "0.0.0.0";
		//String clientIP = "143.129.81.13", serverIP = "143.129.81.13";
		//String clientIP = "192.168.11.1", serverIP = "192.168.11.1"; //hardcoded values for host2.click user
		// clientIP = 192.168.10.1 for ipnetwork.click user
		int serverPort = 6789, clientPort = 2345;
		Scanner in = new Scanner(System.in);

		// if IP's are given as command line arguments
		if (argv.length == 2) {
			clientIP = argv[0];
			serverIP = argv[1];
			System.out.println("Got clientIP=" + clientIP + " and serverIP=" + serverIP + " from command line argumets");
		} else {
			/*
			 * System.out.println("Enter the IP address of the server.");
			 * serverIP = in.nextLine(); System.out.println("Enter the IP address the server will need to connect to.");
			 * clientIP = in.nextLine(); System.out.println("Got clientIP=" + clientIP + " and serverIP=" + serverIP);
			 */
		}

		Server clientResponder = null;
		AppClient clientRequester = null;
		SaslSocketTransceiver transceiver = null;
		AppServerInterface appServer = null;

		// connect to the server to create the appServer proxy object.
		try {
			transceiver = new SaslSocketTransceiver(new InetSocketAddress( serverIP, serverPort));
			
			appServer = (AppServerInterface) SpecificRequestor.getClient( AppServerInterface.class, transceiver);

			// try multiple clientPorts in case the port is already in use
			while (true) {
				// create a clientResponder so the appServer can invoke methods on this client.
				try {
					clientRequester = new AppClient(appServer, clientIP, clientPort, serverIP, serverPort);
					clientResponder = new SaslSocketServer( new SpecificResponder(AppClientInterface.class, clientRequester), new InetSocketAddress(clientPort) );
					clientResponder.start();
					System.out.println("Listening on ip " + clientIP + " and port " + clientPort);
					break;
				} catch (java.net.BindException e) {
					if (clientPort < 65535) {
						clientPort++;
					} else {
						System.err.println("\n > Failed to find open port, quitting program.");
						System.exit(1);
					}
				}
			}

			String username = clientRequester.registerUser(in);

			//TODO: remove on release
			// temporary for testing on localhost
			if (clientPort == 2345) {
				clientRequester.initJFrames(50, 250, 400, 300);
			} else {
				clientRequester.initJFrames(50, 650, 400, 300);
			}

			printWelcome();
			Timer timer = new Timer();
			timer.schedule(clientRequester, 0, 2000);
			ShellFactory.createConsoleShell("chat-app", "", clientRequester).commandLoop();
			timer.cancel();
			System.out.println("Quit program.");

			appServer.unregisterClient(username);
			clientResponder.close();
			in.close();
			transceiver.close();

		} catch (AvroRemoteException e) {
			System.err.println("Apache Avro error.");
		} catch (IOException e) {
			System.err.println("Error connecting to server.");
		}

	}
}
