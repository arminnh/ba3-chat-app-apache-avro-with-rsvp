package server;

import java.util.*;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;

import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.*;
import org.apache.avro.AvroRemoteException;

public class AppServer extends TimerTask implements AppServerInterface {
	private Map<String, ClientInfo> clients = new HashMap<String, ClientInfo>();
	private List<Request> requests = new ArrayList<Request>();

	//======================================================================================

	@Override
	public int registerClient(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		System.out.println("User " + username + " registered at " + ipaddress + " on port " + port);
		ClientInfo client = new ClientInfo(username, ipaddress, port);
		
		// if client has already disconnected, proxy.function will throw an AvroRemoteException
		try {
			client.proxy.echo(666);
		} catch (AvroRemoteException e) {
			return 1;
		}
		
		this.clients.put(username.toString(), client);
		this.printClientList();
		return 0;
	}

	/*
	 *  INVOKABLE METHODS
	 */

	@Override
	public int echo(int message) throws AvroRemoteException {
		return message;
	}

	@Override
	public boolean isNameAvailable(CharSequence username) throws AvroRemoteException {
		return !this.clients.containsKey(username.toString());
	}

	@Override
	public int unregisterClient(CharSequence username) throws AvroRemoteException {
		System.out.println("Removing user: " + username.toString());

		// remove all requests with the client, then remove them from the clients map
		if (this.removeRequestsWithUser(username.toString()) ||
			this.clients.remove(username.toString()) != null) {
			return 2;
		}
		
		this.printClientList();
		return 0;
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
	public int setClientState(CharSequence username, ClientStatus state) throws AvroRemoteException {
		ClientInfo client = this.clients.get(username.toString());
		
		// if client exists
		if (client != null) {
			client.status = state;
			return 0;
		}
		
		return 3;
	}

	@Override
	public int sendMessage(CharSequence username, CharSequence message) throws AvroRemoteException {
		Date dNow = new Date( );
		SimpleDateFormat dFormat = new SimpleDateFormat ("hh:mm:ss");
		String time = dFormat.format(dNow);

		for (Map.Entry<String, ClientInfo> client : this.clients.entrySet()){

			// if client is in public chat, send the message to them if they are not the sender
			if (client.getValue().status == ClientStatus.PUBLIC && !client.getKey().equals(username.toString())) {
				System.out.println("Sending message to " + client.getKey());
				client.getValue().proxy.receiveMessage(time + " " + username + ": " + message);
			}
		}
		
		return 0;
	}

	@Override
	public int sendRequest(CharSequence username1, CharSequence username2) throws AvroRemoteException {
		// if request already exists, return error value
		if (this.getRequest(username1, username2) != null) {
			return 4;
		}
		
		if (this.clients.containsKey(username2.toString())) {
			this.requests.add(new Request(username1.toString(), username2.toString(), RequestStatus.pending));
			this.clients.get(username2.toString()).proxy.receiveMessage("You have received a private chat request from " + username1.toString() + ".");
			return 0;
		}

		// if user does not exist, return error value
		return 3;
	}

	@Override
	public int cancelRequest(CharSequence from, CharSequence to) throws AvroRemoteException {
		if (this.removeRequest(from, to) == 0) {
			this.clients.get(to.toString()).proxy.receiveMessage(from.toString() + " has cancelled their request.");
			return 0;
		}
		
		return 5;
	}

	@Override
	public int removeRequest(CharSequence from, CharSequence to) {
		for (Iterator<Request> it = this.requests.iterator(); it.hasNext();) {
			Request r = it.next();
			
			// if request was found, remove it
			if (r.getFrom().equals(from.toString()) && r.getTo().equals(to.toString())) {
				it.remove();
				return 0;
			}
		}
		
		return 6;
	}

	@Override
	public int requestResponse(CharSequence from, CharSequence to, boolean responseBool) throws AvroRemoteException {
		System.out.println("Entered requestResponse");
		Request r = this.getRequest(from, to);

		// if request does not exist
		if (r == null) {
			return 5;
		}

		ClientInfo requester = this.clients.get(from.toString());
		// if the request has been accepted
		if (responseBool) {
			ClientInfo accepter  = this.clients.get(to.toString());
			
			// if the requester is not in another private chat by now
			if (requester.status != ClientStatus.PRIVATE) {
				System.out.println("Accepter: " + to.toString() + " Requester: " + from.toString());

				r.setStatus(RequestStatus.accepted);
				requester.proxy.receiveMessage(to.toString() + " has accepted your request.");

				System.out.println(requester.clientIP);
				accepter.proxy.setPrivateChatClient(from, requester.clientIP, requester.clientPort);
				requester.proxy.setPrivateChatClient(to, accepter.clientIP, accepter.clientPort);
				
				return 0;
			} else {
				requester.proxy.receiveMessage(to.toString() + " accepted your request, but you seem to be busy.\nYou received a request which you can accept when you're done.");
				accepter.proxy.receiveMessage(from.toString() + " is already in a private chat right now. We sent them a new request.");
				this.removeRequest(from.toString(), to.toString());
				this.sendRequest(to, from);
				
				return 7;
			}
		} else {
			requester.proxy.receiveMessage(to.toString() + " has declined your request.");
			r.setStatus(RequestStatus.declined);
			this.removeRequest(from.toString(), to.toString());
			
			return 0;
		}
	}

	@Override
	public CharSequence getMyRequests(CharSequence username) throws AvroRemoteException {
		String clientUsername = username.toString();
		CharSequence requests = "Requests:\n\t";
		boolean hasRequests = false;

		for (Request r : this.requests) {
			if (r.getFrom().equals(clientUsername) || r.getTo().equals(clientUsername)) {
				String user = r.getFrom().equals(clientUsername) ? r.getTo() : r.getFrom();
				String from = r.getFrom().equals(clientUsername) ? "  to: " : "from: ";

				requests = requests + from + user + ", Status: " + r.getStatus().toString() + "\n\t";
				hasRequests = true;
			}
		}
		
		if (!hasRequests) {
			requests = requests + "No requests";
		}

		return requests;
	}

	@Override
	public boolean isRequestPending(CharSequence username) throws AvroRemoteException {
		for (Request r : this.requests) {
			if (r.getTo().equals(username.toString())) {
				return true;
			}
		}
		
		return false;
	}

	/*
	 * OTHER METHODS
	 */

	public boolean removeRequestsWithUser(String username) {
		boolean removed = false;
		
		for (Iterator<Request> it = this.requests.iterator(); it.hasNext();) {
			Request r = it.next();
			if (r.getFrom().equals(username) || r.getTo().equals(username)) {
				it.remove();
				removed = true;
			}
		}
		
		return removed;
	}
	
	public Request getRequest(CharSequence from, CharSequence to) {
		for (Request r : this.requests) {
			if (r.getFrom().equals(from.toString()) && r.getTo().equals(to.toString())) {
				return r;
			}
		}
		return null;
	}

	public void printClientList() {
		System.out.println("List of registered clients:");
		for (Map.Entry<String, ClientInfo> client : this.clients.entrySet()) {
			System.out.println("\tUsername: " + client.getKey() + "\tStatus: " + client.getValue().status);
		}
		System.out.println();
	}

	public void checkConnectedList() {
		for (Iterator<ClientInfo> iterator = this.clients.values().iterator(); iterator.hasNext();) {
			ClientInfo client = iterator.next();

			// if client has disconnected, proxy.function will throw an AvroRemoteException
			try {
				client.proxy.echo(666);
			} catch (AvroRemoteException e) {
				System.out.println("User: " + client.username + " disconnected. Removed from list.");
				this.removeRequestsWithUser(client.username);
				iterator.remove();
			}
		}
		System.out.println("checkConnectedList, connected users: " + this.clients.size());
	}

	// function that will be ran periodically by a Timer
	public void run() {
		this.checkConnectedList();
	}
	
	public static void main(String[] argv) { 
		Server server = null;
		AppServer appServer = new AppServer();
		Timer timer = new Timer();

		try {
			// open a SaslSocketServer to listen to incoming requests
			server = new SaslSocketServer( new SpecificResponder(AppServerInterface.class, appServer), new InetSocketAddress(6789) );
			System.out.println("Initialized SaslSocketServer");
		} catch (IOException e) {
			System.err.println("[error] Failed to start server");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		server.start();
		timer.schedule(appServer, 0, 3000);

		try {
			server.join();
		} catch (InterruptedException e) {

		}
	}

}
