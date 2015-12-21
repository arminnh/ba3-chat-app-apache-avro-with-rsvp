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
	private List<Request> requests = new ArrayList<Request>(); // {from, to, status}

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
	
	public void removeRequestsWithUser(String username) {
		for (Iterator<Request> it = this.requests.iterator(); it.hasNext();) {
			Request r = it.next();
			if (r.getFrom().equals(username) || r.getTo().equals(username)) {
				it.remove();
			}
		}
	}
	
	@Override
	public int removeRequest(CharSequence from, CharSequence to) {
		for (Iterator<Request> it = this.requests.iterator(); it.hasNext();) {
			Request r = it.next();
			if (r.getFrom().equals(from.toString()) && r.getTo().equals(to.toString())) {
				it.remove();
				return 0;
			}
		}
		return 1;
	}
	
	@Override
	public int exitClient(CharSequence username) throws AvroRemoteException {
		System.out.println("Removing user: " + username.toString());
		
		//remove returns null if element doesnt exist 
		removeRequestsWithUser(username.toString());
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
		for (Iterator<ClientInfo> iterator = this.clients.values().iterator(); iterator.hasNext();) {
			ClientInfo client = iterator.next();
			
			try {
				client.proxy.echo(666);
			} catch (AvroRemoteException e) {
				System.out.println("User: " + client.username + " disconnected. Removed from list.");
				iterator.remove();
			}
			
		}
		System.out.println("checkConnectedList, connected users: " + this.clients.size());
	}
	
	public void run() {
		this.checkConnectedList();
	}
	
	public Request getRequest(CharSequence from, CharSequence to) {
		for (Request r : this.requests) {
			if (r.getFrom().equals(from.toString()) && r.getTo().equals(to.toString())) {
				System.out.println("Returning request from " + from.toString() + " to " + to.toString());
				return r;
			}
		}
		return null;
	}
	
	@Override
	public int sendRequest(CharSequence username1, CharSequence username2) throws AvroRemoteException {
		// TODO: request bestaat al
		if (this.getRequest(username1, username2) != null) {
			return 1;
		}
		this.requests.add(new Request(username1.toString(), username2.toString(), RequestStatus.pending));
		this.clients.get(username2.toString()).proxy.receiveMessage("You have received a private chat request from " + username1.toString() + ".");
		return 0;
	}

	@Override
	public int requestResponse(CharSequence username1, CharSequence username2, boolean responseBool) throws AvroRemoteException {
		//TODO: request bestaat niet
		System.out.println("Entered requestResponse");
		Request r = getRequest(username2, username1);
		
		if (r == null) {
			System.out.println("r == null");
			return 1;
		}
		
		System.out.println("r != null");
	
		ClientInfo requester = this.clients.get(username2.toString());
		if (responseBool) {
			ClientInfo accepter  = this.clients.get(username1.toString());
			if (requester.status != ClientStatus.PRIVATE) {
				System.out.println("Accepter: " + username1.toString() + " Requester: " + username2.toString());
				r.setStatus(RequestStatus.accepted);
				//TODO: ook ergens verwijderen
				
				requester.proxy.receiveMessage(username1.toString() + " accepted your request.");
				
				String[] ipAndPort = requester.address.toString().split(":");
				accepter.proxy.setPrivateChatClient(username2, (CharSequence) ipAndPort[0], Integer.parseInt(ipAndPort[1]));
				ipAndPort = accepter.address.toString().split(":");
				requester.proxy.setPrivateChatClient(username1, (CharSequence) ipAndPort[0], Integer.parseInt(ipAndPort[1]));
				return 2;
			} else {
				requester.proxy.receiveMessage(username1.toString() + " accepted your request, but you seem to be busy.\nYou received a request which you can accept when you're done.");
				accepter.proxy.receiveMessage(username2.toString() + " is already in a private chat right now. We sent them a new request.");
				this.removeRequest(username2.toString(), username1.toString());
				this.sendRequest(username1, username2);
				return 1;
			}
		} else {
			requester.proxy.receiveMessage(username1.toString() + " declined your request.");
			r.setStatus(RequestStatus.declined);
			this.removeRequest(username2.toString(), username1.toString());
		}
		
 		return 0;
	}

	@Override
	public CharSequence getMyRequests(CharSequence username) throws AvroRemoteException {
		String myUsername = username.toString();
		CharSequence requests = "Requests:\n\t";
		
		for (Request r : this.requests) {
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
		timer.schedule(appServer, 0, 3000);
		
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

	@Override
	public int cancelRequest(CharSequence username1, CharSequence username2) throws AvroRemoteException {
		for (Iterator<Request> it = this.requests.iterator(); it.hasNext();) {
			Request r = it.next();
			if (r.getFrom().equals(username1.toString()) && r.getTo().equals(username2.toString())) {
				it.remove();
				break;
			}
		}
		this.clients.get(username2.toString()).proxy.receiveMessage(username1.toString() + " has cancelled their request.");
		return 0;
	}
}
