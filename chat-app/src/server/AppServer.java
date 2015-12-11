package server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.avro.AvroRemoteException;
import client.AppClientInterface;

/*			MINIMUM VEREISTEN
 * - client registreren: uniek ID (long int) en gebruikersnaam voor elke client
 * - voor verbonden gebruikers: ID koppelen aan IP en poortnummer
 * - publieke chatroom voor alle verbonden gebruikers: joinen, berichten ontvangen
 *   en versturen, verlaten
 * - afhandelen uitnodigingen prive gesprekken
 * - twee clients verbinden voor prive gesprek
 * - clients evenwaardig aan elkaar (?), P2P
 * - lijst met openstaande requests bijhouden
 */
public class AppServer extends TimerTask implements AppServerInterface {
	private Map<String, ClientInfo> clients = new HashMap<String, ClientInfo>();
	private List<Request> requestList = new ArrayList<Request>(); // {from, to, status}

	@Override
	public int registerClient(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		//TODO: nakijken of naam uniek is
		ClientInfo client = new ClientInfo();
		client.status = ClientStatus.LOBBY;

		//proxy client
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ipaddress.toString());
			client.address = new InetSocketAddress(addr, port);
			client.transceiver = new SaslSocketTransceiver(client.address);
			System.out.println("transceiver connected: " + client.transceiver.isConnected());
			client.proxy = (AppClientInterface) SpecificRequestor.getClient(AppClientInterface.class, client.transceiver);
			System.out.println("transceiver connected: " + client.transceiver.isConnected());
		} catch (UnknownHostException e) {	//InetAddress.getByName
			e.printStackTrace();
		}catch (IOException e) {			//SaslSocketTransceiver and SpecificRequestor
			e.printStackTrace();
		}

		System.out.println("User " + username + " registered at " + ipaddress + " on port " + port);
		this.clients.put(username.toString(), client);
		this.printClientList();
		
		return 0;
	}

	@Override
	public boolean isNameAvailable(CharSequence username) throws AvroRemoteException {
		return !this.clients.containsKey(username.toString());
	}
	
	@Override
	public int exitClient(CharSequence username) throws AvroRemoteException {
		System.out.println("Removing user: " + username.toString());
		
		//remove returns null if element doesnt exist 
		this.clients.remove(username.toString());
		
		this.printClientList();
		return 1;
	}

	public void printClientList() {
		System.out.println("List of registered clients:");
		for (Map.Entry<String, ClientInfo> client : this.clients.entrySet()) {
			System.out.println("\tUsername: " + client.getKey() + "\tStatus: " + client.getValue().status);
		}
		System.out.println();
	}
	

	@Override
	public CharSequence getListOfClients() throws AvroRemoteException {
		String clients = "List of users: \n";
		
		for (Map.Entry<String, ClientInfo> client : this.clients.entrySet()) {
			clients += "\t\tUsername: " + client.getKey() + "\t\tStatus: " + client.getValue().status + "\n";
		}
		
		return clients;
	}

	@Override
	public int sendMessage(CharSequence username, CharSequence message) throws AvroRemoteException {
		for (Map.Entry<String, ClientInfo> client : this.clients.entrySet()){
			if (client.getValue().status == ClientStatus.PUBLIC && !client.getKey().equals(username.toString())) {
				System.out.println("Sending message to " + client.getKey());
				client.getValue().proxy.receiveMessage(username + ": " + message);
			}
		}
		return 0;
	}

	public void checkConnectedList() {
		System.out.println("checkConnectedList, connected users: " + this.clients.size());
		
		for (Iterator<ClientInfo> iterator = this.clients.values().iterator(); iterator.hasNext();) {
			ClientInfo client = iterator.next();
			//.isConnected() geeft natuurlijk nooit true 
			if (!client.transceiver.isConnected()) {
				/*
				System.out.println("User: " + client.username + " disconnected. Removed from list.");
				System.out.println("client.transceiver.getRemoteName(): " + client.transceiver.getRemoteName());
				try {
					client.proxy.receiveMessage("yo");
				} catch (AvroRemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				
				//iterator.remove();
			}
		}
	}
	
	public void run() {
		this.checkConnectedList();
	}
	
	public Request getRequest(CharSequence from, CharSequence to) {
		for (Request r : this.requestList) {
			if (r.getFrom().equals(from.toString()) && r.getTo().equals(to.toString())) {
				System.out.println("Returning request from " + from.toString() + " to " + to.toString());
				return r;
			}
		}
		return null;
	}
	
	@Override
	public int sendRequest(CharSequence username1, CharSequence username2) throws AvroRemoteException {
		Request request = new Request(username1.toString(), username2.toString(), RequestStatus.pending);
		this.requestList.add(request);
		return 0;
	}

	@Override
	public int requestResponse(CharSequence username1, CharSequence username2, boolean responseBool) throws AvroRemoteException {
		System.out.println("Entered requestResponse");
		Request r = getRequest(username2, username1);
		
		if (r != null) {
			System.out.println("r != null");
			// RequestStatus {pending, accepted, declined, deleted};
			//TODO: kijk eerst na of de andere persoon nog niet in private chat zit
			r.setStatus(responseBool ? RequestStatus.accepted : RequestStatus.declined);
		
			if (responseBool) {
				System.out.println("Accepter: " + username1.toString() + " Requester: " + username2.toString());
				ClientInfo accepter  = this.clients.get(username1.toString());
				ClientInfo requester = this.clients.get(username2.toString());
				
				String[] ipAndPort = requester.address.toString().split(":");
				accepter.proxy.setPrivateChatClient(username2, (CharSequence) ipAndPort[0], Integer.parseInt(ipAndPort[1]));
				ipAndPort = accepter.address.toString().split(":");
				requester.proxy.setPrivateChatClient(username1, (CharSequence) ipAndPort[0], Integer.parseInt(ipAndPort[1]));
			}
		} else {
			System.out.println("r == null");
		}
 		return 0;
	}

	@Override
	public CharSequence getMyRequests(CharSequence username) throws AvroRemoteException {
		String myUsername = username.toString();
		CharSequence requests = "Requests:\n\t";
		
		for (Request r : requestList) {
			if (r.getFrom().equals(myUsername) || r.getTo().equals(myUsername)) {
				String user = r.getFrom().equals(myUsername) ? r.getTo() : r.getFrom();
				String from = r.getFrom().equals(myUsername) ? "  to: " : "from: ";
				
				requests = requests + from + user + ", Status: " + r.getStatus().toString() + "\n\t";
			}
		}
		
		return requests;
	}
	
	public static void main(String[] argv) { 
		Server server = null;
		AppServer appServer = new AppServer();
		Timer timer = new Timer();
		
		try {
			server = new SaslSocketServer( new SpecificResponder(AppServerInterface.class, appServer), new InetSocketAddress(6789) );
			System.out.println("Initialized SaslSocketServer");
		} catch (IOException  e) {
			System.err.println("[error] Failed to start server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		server.start();
		timer.schedule(appServer, 0, 15000);
		
		try {
			//appServer.checkConnectedUsers();
			server.join();
		} catch (InterruptedException e) {
			
		}
	}

	@Override
	public int setClientState(CharSequence username, ClientStatus state) throws AvroRemoteException {
		ClientInfo client = this.clients.get(username.toString());
		if (client != null) {
			client.status = state;
		}
		return 0;
	}
}
