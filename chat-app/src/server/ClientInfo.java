package server;

import java.net.InetSocketAddress;
import chat_app.AppClientInterface;

import org.apache.avro.ipc.*;

enum ClientStatus {LOBBY, PUBLICCHAT, PRIVATECHAT}

public class ClientInfo {
	public int id;
	public CharSequence username;
	public ClientStatus status;
	
	//InetSocketAddress: It provides an immutable object used by sockets for binding, connecting, or as returned values.
	public InetSocketAddress connection;
	public SaslSocketServer socket;
	
	//proxy client
	AppClientInterface proxy = null;
}