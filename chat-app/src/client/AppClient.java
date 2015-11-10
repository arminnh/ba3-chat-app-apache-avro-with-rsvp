package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import asg.cliche.Command;
import asg.cliche.ShellFactory;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
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
	private static Transceiver client = null;
	private static AppServerInterface proxy = null;
	private static int id = -1, port = -1;
	private static String hostIpaddress;
	
	@Command
	public static void register(String username) throws AvroRemoteException {
		id = proxy.registerClient(username, hostIpaddress, port);
		System.out.println("Recieved id: " + id);
	}

	@Command
	public static void exit() throws IOException {
		proxy.exitClient(id);
		client.close();
	}

	@Command
	public static void getListOfUsers() throws IOException {
		CharSequence list = proxy.getListOfClients();
		System.out.println(list);
	}

	@Command
	public static void joinPublicChat() throws IOException {
		proxy.joinPublicChat(id);
	}

	@Command
	public static void sendMessage(String str) throws IOException {
		proxy.sendMessage(id, str);
	}

	@Command
	public static void exitPublicChat() throws IOException {
		proxy.exitPublicChat(id);
	}
	
	@Override
	public int recieveMessage(CharSequence message) throws AvroRemoteException {
		System.out.println(message);
		return 0;
	}
	
	public static void main(String[] argv) {
		//Take server's ipaddress and port from terminal arguments:
		//	ipaddress = argv[0];
		//	port = argv[1];
		//Get hosts ipadress by:
		//	InetAddress addr = new InetAddress.getLocalHost();
		hostIpaddress = "0.0.0.0";
		port = 6789;
		
		try {
			//Setup Transceiver client  and  AppServerInterface proxy
			//addr = InetAddress.getByName(ipaddress);
			//client = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			client = new SaslSocketTransceiver(new InetSocketAddress(port));
			proxy = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class, client);
			System.out.println("Welcome to Chat App, type ?list to get a list of available commands.");

	        ShellFactory.createConsoleShell("chat-app", "", new AppClient()).commandLoop();
			
			System.out.println("Client exit program.");
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