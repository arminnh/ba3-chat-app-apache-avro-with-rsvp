package rsvp;

import java.io.IOException;
import java.net.InetAddress;

import controlsocket.ClickException;
import controlsocket.ControlSocket;

public class RSVP {
	private ControlSocket _controlSocket;
	String _elementName;
	ControlSocket.HandlerInfo _sessionHandler;
	
	public RSVP(InetAddress address, int port, String elementName) throws IOException {
		_controlSocket = new ControlSocket(address, port);
		_elementName = elementName;
	}
	
	public int sender() {
		try {
			_controlSocket.write(_elementName, "session", "DEST 192.168.11.1, PROTOCOL 17, POLICE false, PORT 2222");
			_controlSocket.write(_elementName, "timevalues", "REFRESH 3");
			_controlSocket.write(_elementName, "senderdescriptor", "SRC_ADDRESS 192.168.10.1, SRC_PORT 7, TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "path", "REFRESH true");
		} catch (ClickException e) {
			System.err.println("Setting session unsuccessful");
			e.getCause();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public static void main(String[] args) {
		try {
			RSVP rsvp = new RSVP(InetAddress.getByName("localhost"), 10000, "host1/rsvp");
			rsvp.sender();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
