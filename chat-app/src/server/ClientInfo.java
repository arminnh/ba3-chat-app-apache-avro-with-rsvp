package server;

import java.io.IOException;
import java.net.*;
import org.apache.avro.ipc.*;
import org.apache.avro.ipc.specific.SpecificRequestor;

import client.AppClientInterface;

public class ClientInfo {
	public String username;
	public ClientStatus status;
	public CharSequence clientIP;
	public int clientPort;
	public SaslSocketTransceiver transceiver;
	public AppClientInterface proxy = null;

	public ClientInfo(CharSequence username, CharSequence ipaddress, int port) {
		this.status = ClientStatus.LOBBY;
		this.username = username.toString();
		this.clientIP = ipaddress;
		this.clientPort = port;

		InetAddress addr;
		// try to connect to the client and save a proxy object for them
		try {
			addr = InetAddress.getByName(ipaddress.toString());
			InetSocketAddress address = new InetSocketAddress(addr, port);
			this.transceiver = new SaslSocketTransceiver(address);
			this.proxy = (AppClientInterface) SpecificRequestor.getClient(AppClientInterface.class, this.transceiver);

		} catch (UnknownHostException e) { // InetAddress.getByName
			e.printStackTrace();
		} catch (IOException e) { // SaslSocketTransceiver and SpecificRequestor
			e.printStackTrace();
		}
	}
}