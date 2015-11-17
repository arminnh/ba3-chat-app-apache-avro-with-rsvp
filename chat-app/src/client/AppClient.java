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
 * - requests voor prive gesprekken sturen, accepteren, weigeren
 *   (in elke situatie: publieke chatroom, prive gesprek)
 * - video streaming
 * - maximaal 1 conversatie tegelijk
 */

public class AppClient implements AppClientInterface {
	private SaslSocketTransceiver transceiver = null;
	private AppServerInterface appServer = null;
	private int id = -1, port;
	private String hostIpAddress;
	
	public AppClient(SaslSocketTransceiver t, AppServerInterface a, String hostIpAddress, int port) {
		this.transceiver = t;
		this.appServer = a;
		this.hostIpAddress = hostIpAddress;
		this.port = port;
	}
	
	public void register(String username) throws AvroRemoteException {
		id = appServer.registerClient((CharSequence) username, hostIpAddress, port);
		System.out.println("Recieved id: " + id);
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
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		String input = br.readLine();
		while (!input.equals("?exit")) {
			if (input.equals("?list")) {
				System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
				System.out.println("To leave the chatroom: josse kies maar welke command je hier wil hebben");
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
	
	/*
	 * doen werken op 2 pc's -> clients ip adress mee geven?
	 * getlocalhost -> altijd  0.0.0.0 ??
	 * registratie automatiseren	-> DONE
	 */
	
	public static void main(String[] argv) {
		//Take server's ipaddress and port from terminal arguments:
		//	ipaddress = argv[0];
		//	port = argv[1];
		//Get hosts ipadress by:
		//	InetAddress addr = new InetAddress.getLocalHost();
		String hostIpAddress = "0.0.0.0";
		int port = 6789, clientPort = 2345;

		// http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
		try {
			InetAddress IP = InetAddress.getLocalHost();
			System.out.println(IP.getHostAddress());
			System.out.println(IP.toString());
	        System.out.println("i.isAnyLocalAddress(): " + IP.isAnyLocalAddress());
	        System.out.println("i.isLinkLocalAddress(): " + IP.isLinkLocalAddress());
	        System.out.println("i.isLoopbackAddress(): " + IP.isLoopbackAddress() + "\n");
			
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
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		String username;
		Scanner in = new Scanner(System.in);
		System.out.println("Enter your username.");
		username = in.nextLine();

		System.out.println("Enter the IP address of the server.");
		InetSocketAddress serverIP = new InetSocketAddress(in.nextLine(), port);
		
		System.out.println("Enter the public IP address the server will need to connect to.");
		hostIpAddress = in.nextLine();
		
		Server clientResponder = null;
		AppClient clientRequester = null;
		try {
			//Setup Transceiver client  and  AppServerInterface proxy
			//addr = InetAddress.getByName(ipaddress);
			//client = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(serverIP);
			System.out.println("transceiver connected: " + transceiver.isConnected());
			System.out.println("transceiver.getRemoteName(): " + transceiver.getRemoteName());
			AppServerInterface appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);
			System.out.println("transceiver connected: " + transceiver.isConnected());
			
			while (true) {
				System.out.println("Trying to use port " + clientPort);
				try {
					clientRequester = new AppClient(transceiver, appServer, hostIpAddress, clientPort);
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

			//appServer.registerClient(username,  hostIpAddress,  clientPort);
			clientRequester.register(username);
			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");
	        ShellFactory.createConsoleShell("chat-app", "", clientRequester).commandLoop();
			System.out.println("Client exit program.");
			
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
}
// Runtime.getRuntime().addShutdownHook