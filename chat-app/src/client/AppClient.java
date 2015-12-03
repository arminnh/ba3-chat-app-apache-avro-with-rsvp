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
import java.util.Scanner;

import chat_app.AppServerInterface;
import chat_app.AppClientInterface;

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
	private int id = -1, port;
	private String hostIP;
	
	private String serverIP;
	private int serverPort;
	
	private server.ClientInfo PrivateChatClient = null;
	
	private BufferedReader br = null;
	
	public AppClient(SaslSocketTransceiver t, AppServerInterface a, String hostIP, int port) {
		this.transceiver = t;
		this.appServer = a;
		this.hostIP = hostIP;
		this.port = port;
	}
	
	public void register(String username) throws AvroRemoteException {
		id = appServer.registerClient((CharSequence) username, hostIP, port);
		//System.out.println("Recieved id: " + id);
	}

	@Command
	public void exit() throws IOException {
		appServer.exitClient(id);
		transceiver.close();
	}

	@Command
	public void getListOfUsers() throws IOException {
		CharSequence list = appServer.getListOfClients();
		System.out.println(list);
	}

	@Command
	public void joinPublicChat() throws IOException {
		appServer.joinPublicChat(id);
		
		System.out.println("You entered the public chatroom.\nThe commands are different here, type ?list to get the list of commands.");
		
		br = new BufferedReader(new InputStreamReader(System.in)); 
		String input = br.readLine();
		//br.close();
		
		while (!input.equals("?leave") && !input.equals("?q") ) {
			
			if (input.equals("?list")) {
				System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
				System.out.println("To leave the chatroom:              ?leave or ?q");
			} else if (input.equals("?getListOfUsers") || input.equals("?glou")) {
				getListOfUsers();
			} else {
				appServer.sendMessage(id, input);
			}
			input = br.readLine();
		}
		appServer.exitPublicChat(id);
		System.out.println("Left public chat.");
	}

	public void sendMessage(CharSequence str) throws IOException {
		appServer.sendMessage(id, str);
	}

	public void exitPublicChat() throws IOException {
		appServer.exitPublicChat(id);
	}
	
	@Override
	public int receiveMessage(CharSequence message) throws AvroRemoteException {
		System.out.println(message);
		return 0;
	}
	
	public static void main(String[] argv) {
		//Take server's ipaddress and port from terminal arguments:
		//	ipaddress = argv[0];
		//	port = argv[1];
		//Get hosts ipadress by:
		//  http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
		//  -> Just ask the user for now
		String hostIP = "0.0.0.0";
		int port = 6789, clientPort = 2345;

		try {
			InetAddress IP = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} 
		
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
		
		String username;
		Scanner in = new Scanner(System.in);
		System.out.println("Enter your username.");
		username = in.nextLine();

		System.out.println("Enter the IP address of the server.");
		InetSocketAddress serverIP = new InetSocketAddress(in.nextLine(), port);
		
		System.out.println("Enter the IP address the server will need to connect to.");
		hostIP = in.nextLine();
		
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
					clientRequester = new AppClient(transceiver, appServer, hostIP, clientPort);
					clientResponder = new SaslSocketServer( new SpecificResponder(AppClientInterface.class, clientRequester), new InetSocketAddress(clientPort) );
					clientResponder.start();
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

			clientRequester.register(username);
			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");
	        ShellFactory.createConsoleShell("chat-app", "", clientRequester).commandLoop();
			System.out.println("Quit program.");
			
			//clientResponder.join();
			clientResponder.close();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error connecting to server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
	}

	@Override
	public int receiveRequest(CharSequence request) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public CharSequence video(CharSequence iets) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int requestAccepted(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int stopPrivateChat() throws AvroRemoteException {
		// TODO Auto-generated method stub
		return 0;
	}
}

// Runtime.getRuntime().addShutdownHook