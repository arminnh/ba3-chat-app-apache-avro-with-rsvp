package client;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.Image;
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

public class AppClient extends TimerTask implements AppClientInterface {
	private CharSequence username;
	private ClientStatus status;
	private String clientIP, serverIP;
	private int clientPort, serverPort;
	private AppServerInterface appServer;

	private ClientInfo privateChatClient;
	private boolean privateChatClientArrived, videoRequestPending, videoRequestAccepted, connectedToServer;

	private JFrame senderFrame = new JFrame();
	private JFrame receiverFrame = new JFrame();
	private Graphics receiverG;
	private int reconnectAttempts;

	// ======================================================================================

	public AppClient(AppServerInterface a, String clientIP, int clientPort, String serverIP, int serverPort) {
		this.appServer = a;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.status = ClientStatus.LOBBY;
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
	public int receiveRequest(CharSequence request) throws AvroRemoteException {
		System.out.println(request.toString());
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
		System.out.println("\n > " + this.privateChatClient.username + " has left the private chat. Messages you write will now not be sent.");
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
	public int setFrameVisible(boolean visible) throws AvroRemoteException {
		this.receiverFrame.setVisible(visible);
		if (visible)
			this.receiverG = this.receiverFrame.getGraphics();

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
		this.joinChat();
		System.out.println("\n > " + "You have left the public chat.");
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
		int response = this.appServer.sendRequest((CharSequence) this.username, (CharSequence) username);

		if (response != 0)
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
		if (!this.isConnectedToServer())
			return;

		int response = this.appServer.requestResponse((CharSequence) username, this.username, true);

		// server.requestResponse returned success code
		if (response == 0) {
			this.setStatus(ClientStatus.PRIVATE);
			this.privateChatClient.proxy .receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!");

			System.out.println("\n > " + "You entered a private chatroom with " + this.privateChatClient.username + ". Wait for them to arrive.");
			this.joinPrivateChat();
		} else {
			ErrorWriter.printError(response);
		}
	}

	@Command
	public void declineRequest(String username) throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		int response = this.appServer.requestResponse((CharSequence) username, this.username, false);

		if (response == 0) {
			System.out.println("\n > You have declined the request from" + username + "");
		} else {
			ErrorWriter.printError(response);
		}
	}

	@Command
	public void startPrivateChat() throws AvroRemoteException {
		if (!this.isConnectedToServer())
			return;

		if (!this.appServer.isRequestStatus(this.username, RequestStatus.ACCEPTED)) {
			ErrorWriter.printError(11);
			return;
		}

		this.setStatus(ClientStatus.PRIVATE);

		this.privateChatClientArrived = true;
		this.privateChatClient.proxy.setPrivateChatClientArrived(true);
		this.privateChatClient.proxy.receiveMessage("\n > " + this.username.toString() + " has entered the private chat.");
		this.appServer.removeRequest(this.username, this.privateChatClient.username);

		System.out.println("\n > You entered a private chatroom with " + this.privateChatClient.username);
		this.joinPrivateChat();
	}

	/*
	 * OTHER METHODS
	 */

	public void initJFrames(int x, int y, int width, int height) {
		this.senderFrame = new JFrame();
		this.senderFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.senderFrame.setBounds(x, y, width, height);

		this.receiverFrame = new JFrame();
		this.receiverFrame.getContentPane().add(new JPanel(new BorderLayout()));
		this.receiverFrame.setBounds(x + width + 50, y, width, height);
	}

	public int register(String username) throws AvroRemoteException {
		if (this.appServer.isNameAvailable((CharSequence) username)) {
			this.appServer.registerClient((CharSequence) username, (CharSequence) clientIP, clientPort);
			this.username = username;
			this.connectedToServer = true;
			return 0;
		}

		return 9;
	}

	private boolean isConnectedToServer() {
		if (!this.connectedToServer) {
			System.err.println(" > Not connected to server right now.");
			return false;
		}

		return true;
	}

	private int setStatus(ClientStatus status) throws AvroRemoteException {
		this.appServer.setClientState(this.username, status);
		this.status = status;
		return 0;
	}

	// TEST SWITCHING BETWEEN CHAT MODES
	private int joinChat() throws AvroRemoteException {
		System.out.println(" > The commands are different here, type ?list to get the list of commands.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
			String input = br.readLine().toLowerCase();

			while (!input.matches("(\\?)(leave|q)")) {
				chatCommands(input);

				input = br.readLine();
			}
		} catch (IOException e) {
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

		} else if (input.matches("(\\?)(sendrequest|sr)")) {
			// TODO: check array bounds
			sendRequest(input.split("\\s+")[1]);

		} else if (input.matches("(\\?)(cancelrequest|cr)")) {
			cancelRequest(input.split("\\s+")[1]);

		} else if (input.matches("(\\?)(declinerequest|dr)")) {
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
					     + "Print this list:                 ?list, ?l\n"
					     + "Leave the chatroom:              ?leave, ?q\n");

		if (this.connectedToServer) {
			System.out.println("Get the list of connected users: ?getListOfUsers, ?glou\n"
						     + "List your requests:              ?listMyRequests, ?lmr\n"
						     + "Send a request to X:             ?sendRequest X, ?sr X\n"
						     + "Cancel a request to X:           ?cancelRequest X, ?cr X\n"
						     + "Decline a request from X:        ?declineRequest X, ?dc X\n");

			// only public chat commands
			if (status == ClientStatus.PUBLIC) {
				System.out.println("\n=================          PUBLIC CHAT COMMANDS          ===================\n"
							     + "Accept a request from X:         ?acceptRequest X, ?ar X");

				if (appServer.isRequestStatus(username, RequestStatus.ACCEPTED))
					System.out.println("To start the private chat:       ?joinPrivateChat, ?jpc, ?startPrivateChat, ?spc");
			}
		}

		// only private chat commands
		if (status == ClientStatus.PRIVATE) {
			System.out.println("\n=================          PRIVATE CHAT COMMANDS          ==================\n"
						     + "Go to the the public chatroom:   ?joinPublicChat or ?jpc\n"
						     + "Send a video request:            ?videoRequest, ?vr, ?sendVideoRequest, ?svr");

			if (videoRequestAccepted)
				System.out.println("Send a video:                    ?sendVideo, ?sv");

			if (videoRequestPending) {
				System.out.println("Accept a video request:          ?acceptVideo, ?av\n"
							     + "Decline a video request:         ?declineVideo, ?dv");
			}
		}
	}

	private void publicChatCommands(String input) throws IOException {
		if (input.matches("(\\?)(acceptrequest|ar)")) {
			acceptRequest(input.split("\\s+")[1]);

			// go to the private chat if you have an open request that was accepted
		} else if (input.matches("(\\?)(joinprivatechat|jpc|startprivatechat|spc)") && appServer.isRequestStatus(username, RequestStatus.ACCEPTED)) {
			System.out.println("\n > Left the public chatroom.\n > Joined private chat with " + privateChatClient.username + ".");
			setStatus(ClientStatus.PRIVATE);
			// TODO: handle private chat stuff

			// if no command was detected, send input to everyone in public chat mode
		} else if (!this.isConnectedToServer()) {
			appServer.sendMessage(username, input);
		}
	}

	private void privateChatCommands(String input) throws AvroRemoteException {
		if (input.matches("(\\?)(joinpublicchat|jpc)")) {
			System.out.println("\n > Left the private chat.\n > Joined the public chatroom.");
			setStatus(ClientStatus.PUBLIC);
			// TODO: private chat cleanup
		}

		if (privateChatClientArrived && this.privateChatClient != null) {
			if (input.matches("(\\?)(sendvideorequest|videorequest|svr|vr)")) {
				System.out.println("\n > You have sent a video request.");
				this.privateChatClient.proxy.videoRequest();

			} else if (input.matches("(\\?)(sendvideo|sv)") && videoRequestAccepted) {
				sendVideo();

			} else if (videoRequestPending) {
				if (input.matches("(\\?)(acceptvideo|av)"))
					acceptVideoRequest();
				else if (input.matches("(\\?)(declinevideo|dv)"))
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

	private void declineVideoRequest() throws AvroRemoteException {
		System.out.println("You have declined the video request.");
		this.videoRequestPending = false;
		this.privateChatClient.proxy.receiveMessage("\n > " + this.username.toString() + " has declined the video request.");
	}

	private void acceptVideoRequest() throws AvroRemoteException {
		System.out.println("\n > You have accepted the video request.");
		this.videoRequestPending = false;
		this.privateChatClient.proxy.videoRequestAccepted();
	}

	private void sendVideo() throws AvroRemoteException {
		VideoFileChooser fc = new VideoFileChooser();
		File video = fc.getFile();
		
		try {
			video.canRead();
			this.senderFrame.setVisible(true);
			this.privateChatClient.proxy.setFrameVisible(true);
			VideoSender videoSender = new VideoSender(video, this.senderFrame, this.privateChatClient.proxy);
			Thread sender = new Thread(videoSender);
			sender.start();
		} catch (NullPointerException e) { // from fc.getFile()
			this.privateChatClient.proxy.receiveMessage(" > " + this.username.toString() + " has cancelled the video request.");
			System.out.println(" > You cancelled the video request.");
		}
		
		this.videoRequestAccepted = false;
	}

	private void joinPrivateChat() throws AvroRemoteException {
		//TODO: interrupt other chat on exit
		int response = this.joinChat();

		if (response == 0)
			System.out.println("\n > You have left the private chat.");
		else
			ErrorWriter.printError(response);

		if (this.privateChatClient != null) {
			try {
				if (this.privateChatClientArrived) {
					this.privateChatClient.proxy.leftPrivateChat();
				}
				this.privateChatClient.transceiver.close();
				this.privateChatClient = null;
			} catch (IOException e) {
			}
		}

		this.privateChatClientArrived = false;
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
			System.out.println("\n > Disconnected from private chat. Returning to lobby. ");
			// TODO: try to clean up gracefully
			// TODO: interrupt chat
			this.senderFrame.setVisible(false);
			this.receiverFrame.setVisible(false);
			this.privateChatClient = null;

		}
	}

	private static String registerUser(Scanner in, AppClient clientRequester) throws AvroRemoteException {
		// choose a username and register it with the server
		String username;
		System.out.println(" > Enter your username.");
		while (true) {
			username = in.nextLine();
			if (clientRequester.register(username) == 0) {
				break;
			}
			System.out.println("\n > That name is already taken, choose another one.");
		}
		return username;
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

	public static void main(String[] argv) {
		// String clientIP = "0.0.0.0", serverIP = "0.0.0.0";
		String clientIP = "143.129.81.7", serverIP = "143.129.81.7";
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

			String username = registerUser(in, clientRequester);

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

// Get hosts ipadress by:
// http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
// -> Just ask the user for now

/*
 * try { Enumeration e = NetworkInterface.getNetworkInterfaces();
 * while(e.hasMoreElements()) { NetworkInterface n = (NetworkInterface)
 * e.nextElement(); Enumeration ee = n.getInetAddresses(); while
 * (ee.hasMoreElements()) { InetAddress i = (InetAddress) ee.nextElement();
 * System.out.println(i.getHostAddress());
 * System.out.println("i.isAnyLocalAddress(): " + i.isAnyLocalAddress());
 * System.out.println("i.isLinkLocalAddress(): " + i.isLinkLocalAddress());
 * System.out.println("i.isLoopbackAddress(): " + i.isLoopbackAddress());
 * System.out.println(); } } } catch (SocketException e1) {
 * e1.printStackTrace(); }
 */

// System.out.println("transceiver connected: " + transceiver.isConnected());
// System.out.println("transceiver.getRemoteName(): " +
// transceiver.getRemoteName());

/*
 * clientRequester.register(username); clientRequester.privateChatClient = new
 * ClientInfo();
 * 
 * if (username.equals("a")) { clientPort = 2346;
 * System.out.println("clientport =" + clientPort); } else clientPort = 2345;
 * 
 * while (true) { try { clientRequester.setPrivateChatClient("b", "00.0.0.0",
 * clientPort); break; } catch (Exception e) { if (clientPort < 65535) {
 * clientPort++; } else {
 * System.err.println("Failed to find open port, quitting program.");
 * System.exit(1); } } } clientRequester.startPrivateChat();
 */
