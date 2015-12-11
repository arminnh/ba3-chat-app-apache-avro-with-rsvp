package client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

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

import server.AppServerInterface;

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
	public void respondRequest(String username, boolean responseBool) throws AvroRemoteException {
		this.appServer.requestResponse(this.username, (CharSequence) username, responseBool);
		
		if (responseBool) {
			this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
			this.privateChatClient.proxy.receiveMessage("\n > " + this.username + " has accepted your chat request. Use the command startPrivateChat to start chatting!\n");
			
			try {
				this.joinChat(true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int receiveRequest(CharSequence request) throws AvroRemoteException {
		System.out.println(request.toString());
		return 0;
	}

	@Command
	@Override
	public CharSequence video(CharSequence iets) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command
	public int startPrivateChat() throws AvroRemoteException {
		this.appServer.setClientState(this.username, server.ClientStatus.PRIVATE);
		try {
			this.privateChatClientArrived = true;
			this.privateChatClient.proxy.setPrivateChatClientArrived(true);
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
		/*System.out.println("Enter the IP address of the server.");
		InetSocketAddress serverIP = new InetSocketAddress(in.nextLine(), port);
		
		System.out.println("Enter the IP address the server will need to connect to.");
		clientIP = in.nextLine();*/
		InetSocketAddress serverIP = new InetSocketAddress("0.0.0.0", port);
		
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
}

// Runtime.getRuntime().addShutdownHook