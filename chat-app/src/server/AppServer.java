package server;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.avro.AvroRemoteException;
import chat_app.AppServerInterface;
import chat_app.AppClientInterface;

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
	//ClientInfo = {username, id, status(enum), proxy object}
	private List<ClientInfo> connectedClients = new ArrayList<ClientInfo>();
	private int idCounter = 0;
	private List<Request> requestList = new ArrayList<Request>(); // {from, to, status}

	@Override
	public int registerClient(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		ClientInfo client = new ClientInfo();
		client.username = username;
		client.id = this.idCounter++;
		client.status = ClientStatus.LOBBY;
		client.address = address;

		//proxy client
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ipaddress.toString());
			client.transceiver = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			System.out.println("transceiver connected: " + client.transceiver.isConnected());
			client.proxy = (AppClientInterface) SpecificRequestor.getClient(AppClientInterface.class, client.transceiver);
			System.out.println("transceiver connected: " + client.transceiver.isConnected());
		} catch (UnknownHostException e) {	//InetAddress.getByName
			e.printStackTrace();
		}catch (IOException e) {			//SaslSocketTransceiver and SpecificRequestor
			e.printStackTrace();
		}

		System.out.println("User " + username + " registered at " + ipaddress + " on port " + port);
		this.connectedClients.add(client);
		System.out.println("List of registered users:");
		for (ClientInfo clientt : this.connectedClients) {
			System.out.println("\t ID: " + clientt.id + "\tUsername: " + clientt.username + "\tStatus: " + clientt.status);
		}
		System.out.println();
		
		return client.id;
	}
	
	@Override
	public int exitClient(int id) throws AvroRemoteException {
		System.out.println("Removing user with id: " + id);
		
		for (int i = 0; i < this.connectedClients.size(); i++) {
			ClientInfo clientt = this.connectedClients.get(i);
			if (clientt.id == id) {
				this.connectedClients.remove(i);
				break;
			}
		}
		
		System.out.println("List of registered clients:");
		for (ClientInfo clientt : this.connectedClients) {
			System.out.println("\tID: " + clientt.id + "\tUsername: " + clientt.username + "\tStatus: " + clientt.status);
		}
		System.out.println();
		return 1;
	}

	@Override
	public CharSequence getListOfClients() throws AvroRemoteException {
		String clients = "List of users: \n";
		
		for (ClientInfo client : this.connectedClients) {
			clients += "\tID: " + client.id + "\t\tUsername: " + client.username + "\t\tStatus: " + client.status + "\n";
		}
		
		return clients;
	}

	@Override
	public int joinPublicChat(int id) throws AvroRemoteException {
		ClientInfo client = this.connectedClients.get(id);
		client.status = ClientStatus.PUBLICCHAT;
		this.connectedClients.set(id, client);
		return 1;
	}

	@Override
	public int sendMessage(int id, CharSequence message) throws AvroRemoteException {
		String username = "";
		for (ClientInfo client : this.connectedClients){
			if (client.id == id) {
				username = client.username.toString();
			}
		}
		
		for (ClientInfo client : this.connectedClients){
			if (client.status == ClientStatus.PUBLICCHAT && client.id != id) {
				System.out.println("Sending message to id" + client.id);
				client.proxy.receiveMessage(username + ": " + message);
				
			}
		}
		return 0;
	}

	@Override
	public int exitPublicChat(int id) throws AvroRemoteException {
		ClientInfo client = this.connectedClients.get(id);
		client.status = ClientStatus.LOBBY;
		this.connectedClients.set(id, client);
		return 0;
	}

	public void checkConnectedList() {
		System.out.println("checkConnectedList, connected users: " + this.connectedClients.size());
		
		for (Iterator<ClientInfo> iterator = this.connectedClients.iterator(); iterator.hasNext();) {
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

	@Override
	public int exitPrivateChat(int id) throws AvroRemoteException {
		ClientInfo client = this.connectedClients.get(id);
		client.status = ClientStatus.PUBLICCHAT;
		this.connectedClients.set(id, client);
		return 1;
	}
	
	public ClientInfo getClientInfo(int id) {
		for (ClientInfo c : this.connectedClients) {
			if (c.id == id) {
				return c;
			}
		}
		
		return null;
	}
	
	public Request getRequest(int from, int to) {
		for (Request r : this.requestList) {
			if (r.getFrom() == from && r.getTo() == to) {
				return r;
			}
		}
		return null;
	}
	
	@Override
	public int sendRequest(int id1, int id2) throws AvroRemoteException {
		Request request = new Request(id1, id2, RequestStatus.pending);
		this.requestList.add(request);
		return 0;
	}

	@Override
	public List<CharSequence> requestResponse(int id1, int id2, boolean responseBool) throws AvroRemoteException {
		Request r = getRequest(id2, id1);
		if (r != null) {
			r.setStatus(responseBool ? RequestStatus.accepted : RequestStatus.declined);
		}
		
		List<CharSequence> clientInfo = new ArrayList<CharSequence>();
		
		clientInfo.add(getClientInfo(id2).)
		
 		return ;
	}
	
	public String getUsername(int id) {
		for (ClientInfo client : this.connectedClients) {
			if (client.id == id) {
				return (String) client.username;
			}
		}
		
		return "fuck";
	}

	@Override
	public CharSequence getMyRequests(int id) throws AvroRemoteException {
		CharSequence requests = "Requests:\n\t";
		
		for (Request r : requestList) {
			if (r.getFrom() == id || r.getTo() == id) {
				String username = getUsername((r.getFrom() == id) ? r.getFrom() : r.getTo());
				String from = (r.getFrom() == id) ? "  to" : "from";
				
				requests = requests + from + username + ", Status:" + r.getStatus().toString() + "\n\t";
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
		timer.schedule(appServer, 0, 5000);
		
		try {
			//appServer.checkConnectedUsers();
			server.join();
		} catch (InterruptedException e) {
			
		}
	}
}
