package client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import asg.cliche.Command;
import asg.cliche.ShellFactory;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

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
	private Transceiver transceiver = null;
	private AppServerInterface appServer = null;
	private int id = -1, port;
	private String hostIpAddress;
	
	public AppClient(Transceiver t, AppServerInterface a, String hostIpAddress, int port) {
		this.transceiver = t;
		this.appServer = a;
		this.hostIpAddress = hostIpAddress;
		this.port = port;
	}
	@Command
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
		
		System.out.println("Entered public chat, command zijn hier anders");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		String input = br.readLine();
		while (!input.equals("?exit")) {
			if (input.equals("?list")) {
				System.out.println("To get the list of connected users: ?getListOfUsers or ?glou");
			} else if (input.equals("?getListOfUsers") || input.equals("?glou")) {
				getListOfUsers();
			} else {
				appServer.sendMessage(id, input);
			}
			input = br.readLine();
		}
		appServer.exitPublicChat(id);
		System.out.println("Left public chat, command zijn terug");
	}

	@Command
	public void sendMessage(CharSequence str) throws IOException {
		appServer.sendMessage(id, str);
	}

	@Command
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
		//	InetAddress addr = new InetAddress.getLocalHost();
		String hostIpAddress = "0.0.0.0";
		int port = 6789, clientPort = 1234;

		Server abc = null;
		AppClient def = null;
		try {
			//Setup Transceiver client  and  AppServerInterface proxy
			//addr = InetAddress.getByName(ipaddress);
			//client = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(port));
			AppServerInterface appServer = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, transceiver);
			
			while (true) {
				System.out.println("Trying to use port " + clientPort);
				try {
					def = new AppClient(transceiver, appServer, hostIpAddress, clientPort);
					abc = new SaslSocketServer( new SpecificResponder(AppClientInterface.class, def), new InetSocketAddress(clientPort) );
					abc.start();
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

			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");
	        ShellFactory.createConsoleShell("chat-app", "", def).commandLoop();
			System.out.println("Client exit program.");
			
			abc.join();
			abc.close();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error connecting to server.");
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
// Runtime.getRuntime().addShutdownHook