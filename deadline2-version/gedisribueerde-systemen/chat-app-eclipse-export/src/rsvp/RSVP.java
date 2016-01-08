package rsvp;

import java.io.IOException;
import java.net.InetAddress;

import controlsocket.ClickException;
import controlsocket.ControlSocket;

public class RSVP {
	private ControlSocket _controlSocket;
	private String _elementName;
	private ControlSocket.HandlerInfo _sessionHandler;
	
	public RSVP(InetAddress address, int port, String srcIP, int srcPort) throws IOException, ClickException {
		_controlSocket = new ControlSocket(address, port);
		
		if (_controlSocket.checkHandler("host1/rsvp", "session", true))
			_elementName = "host1/rsvp";
		else if (_controlSocket.checkHandler("host2/rsvp", "session", true))
			_elementName = "host2/rsvp";
		
		System.out.println("_elementName="+_elementName);
	}
	
	public void requestQoS(String srcIP, int srcPort, String dstIP, int dstPort) {
		
		/*System.out.println("session DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
		System.out.println("senderdescriptor SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
		System.out.println("path REFRESH true");*/
		
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "senderdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "path", "REFRESH true");
		} catch (ClickException e) {
			System.err.println("Requesting QoS reservation unsuccesful.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void confirmQoS(String srcIP, int srcPort, String dstIP, int dstPort) {
		
		/*System.out.println("session DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
		System.out.println("flowdescriptor SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
		System.out.println("resv REFRESH true, CONFIRM true");*/
		
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "flowdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "resv", "REFRESH true, CONFIRM true");
		} catch (ClickException e) {
			System.err.println("Confirming QoS reservation unsuccesful.");
			e.getCause();
			System.out.println("stack trace:");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tearPath(String srcIP, int srcPort, String dstIP, int dstPort) {
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "senderdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "pathtear", "");
		} catch (ClickException e) {
			System.err.println("tear path unsuccesful.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tearResv(String srcIP, int srcPort, String dstIP, int dstPort) {
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "flowdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "resvtear", "");
		} catch (ClickException e) {
			System.err.println("tear resv unsuccesful.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
