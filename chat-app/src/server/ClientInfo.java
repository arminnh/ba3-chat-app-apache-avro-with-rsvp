package server;

import java.net.InetSocketAddress;
import chat_app.AppClientInterface;

import org.apache.avro.ipc.*;

enum ClientStatus {LOBBY, PUBLICCHAT, PRIVATECHAT}

public class ClientInfo {
	public int id;
	public CharSequence username;
	public ClientStatus status;
	public SaslSocketTransceiver transceiver;
	public AppClientInterface proxy = null;
	public InetSocketAddress address;
}