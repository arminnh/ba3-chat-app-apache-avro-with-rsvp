package server;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;

import chat_app.ServerInterface;
import chat_app.ClientInterface;

/*
 * - client registreren: uniek ID (long int) en gebruikersnaam voor elke client
 * - voor verbonden gebruikers: ID koppelen aan IP en poortnummer
 * - publieke chatroom voor alle verbonden gebruikers: joinen, berichten ontvangen
 *   en versturen, verlaten
 * - afhandelen uitnodigingen prive gesprekken
 * - twee clients verbinden voor prive gesprek
 * - clients evenwaardig aan elkaar (?), P2P
 * - lijst met openstaande requests bijhouden
 */

public class Server implements ServerInterface {
	//keep a list of (client, status, id, poortnummer)
	private List<ClientInfo> connectedClients;

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
			addr = InetAddress.getByName((String) ipaddress);
			clientTransceiver = new SaslSocketTransceiver( new InetSocketAddress(addr, port) );
			client.proxy = (ClientInterface) SpecificRequestor.getClient(ClientInterface.class, clientTransceiver);
		} catch (UnknownHostException e) {	//InetAddress.getByName
			e.printStackTrace();
		}catch (IOException e) {			//SaslSocketTransceiver and SpecificRequestor
			e.printStackTrace();
		}

		System.out.println("Client " + username + "connected at " + ipaddress + " on port " + port);
		System.out.println("\tClient status" + client.status);
		this.connectedClients.add(client);
		return client.id;
	}
	
	@Override
	public int exitClient(int id) throws AvroRemoteException {
		this.connectedClients.remove(id);
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
		
	}
}
