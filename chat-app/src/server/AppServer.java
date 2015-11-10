package server;
import java.util.List;
import java.util.ArrayList;
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

/*			TEGEN TUSSENTIJDSE EVALUATIE
 * We verwachten dat je volgende features hebt ge ̈ımplementeerd en kan demonstreren:
 * 	Een client moet kunnen registreren bij de server en de applicatie correct kunnen verlaten.
 * 	Een client moet een lijst kunnen opvragen van de verbonden clients.
 * 	Een client moet de publieke chat room kunnen joinen en daarin boodschappen versturen.
*/
public class AppServer implements AppServerInterface {
	//ClientInfo = {username, id, status(enum), proxy object}
	private List<ClientInfo> connectedClients = new ArrayList<ClientInfo>();

	@Override
	public int registerClient(CharSequence username, CharSequence ipaddress, int port) throws AvroRemoteException {
		ClientInfo client = new ClientInfo();
		client.username = username;
		client.id = this.connectedClients.size();
		client.status = ClientStatus.LOBBY;
		//client.connection = new InetSocketAddress();

		//proxy client
		InetAddress addr;
		Transceiver clientTransceiver;
		try {
			//addr = InetAddress.getByName((String) ipaddress);
			//clientTransceiver = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			clientTransceiver = new SaslSocketTransceiver(new InetSocketAddress(6789));
			client.proxy = (AppClientInterface) SpecificRequestor.getClient(AppClientInterface.class, clientTransceiver);
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
		this.connectedClients.remove(id);
		System.out.println("List of registered clients:");
		for (ClientInfo clientt : this.connectedClients) {
			System.out.println("\t ID: " + clientt.id + "\tUsername: " + clientt.username + "\tStatus: " + clientt.status);
		}
		System.out.println();
		return 1;
	}

	@Override
	public List<CharSequence> getListOfClients() throws AvroRemoteException {
		List<CharSequence> clients = new ArrayList<CharSequence>();
		
		for (ClientInfo client : this.connectedClients) {
			clients.add(client.username);
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
		for (ClientInfo client : this.connectedClients){
			//client.proxy.sendMessage()	
			client.proxy.recieveMessage(message);
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

	
	public static void main(String[] argv) { 
		org.apache.avro.ipc.Server server = null;
		
		try {
			server = new SaslSocketServer( new SpecificResponder(AppServerInterface.class, new AppServer()), new InetSocketAddress(6789) );
			System.out.println("Initialized SaslSocketServer");
		} catch (IOException  e) {
			System.err.println("[error] Failed to start server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		server.start();
		System.out.println("Started Server");
		try {
			System.out.println("Listening...");
			server.join();
		} catch (InterruptedException e) {
			
		}
	}
}
