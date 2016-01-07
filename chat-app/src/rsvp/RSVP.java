package rsvp;

import java.io.IOException;
import java.net.InetAddress;

import controlsocket.ClickException;
import controlsocket.ControlSocket;

public class RSVP {
	private ControlSocket _controlSocket;
	String _elementName;
	ControlSocket.HandlerInfo _sessionHandler;
	
	String srcIP, dstIP;
	int srcPort, dstPort;
	
	public RSVP(InetAddress address, int port, String srcIP, int srcPort) throws IOException, ClickException {
		_controlSocket = new ControlSocket(address, port);
		
		if (_controlSocket.checkHandler("host1/rsvp", "session", true))
			_elementName = "host1/rsvp";
		else if (_controlSocket.checkHandler("host2/rsvp", "session", true))
			_elementName = "host2/rsvp";
		
		this.srcIP = srcIP;
		this.srcPort = srcPort;
	}
	
	public void requestQoS(String dstIP, int dstPort) {
		this.dstIP = dstIP;
		this.dstPort = dstPort;
		
		System.out.println("session DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
		System.out.println("timevalues REFRESH 3");
		System.out.println("senderdescriptor SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
		System.out.println("path REFRESH true");
		
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "timevalues", "REFRESH 3");
			_controlSocket.write(_elementName, "senderdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "path", "REFRESH true");
		} catch (ClickException e) {
			System.err.println("Requesting QoS reservation unsuccesful.");
			System.err.println(e.getCause().getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void confirmQoS(String dstIP, int dstPort) {
		this.dstIP = dstIP;
		this.dstPort = dstPort;
		
		System.out.println("session DEST " + srcIP + ", PROTOCOL 6, POLICE false, PORT " + srcPort);
		System.out.println("timevalues REFRESH 3");
		System.out.println("flowdescriptor SRC_ADDRESS " + dstIP + ", SRC_PORT " + dstPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
		System.out.println("resv REFRESH true, CONFIRM true");
		
		try {
			_controlSocket.write(_elementName, "session", "DEST " + srcIP + ", PROTOCOL 6, POLICE false, PORT " + srcPort);
			_controlSocket.write(_elementName, "timevalues", "REFRESH 3");
			_controlSocket.write(_elementName, "flowdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "resv", "REFRESH true, CONFIRM true");
		} catch (ClickException e) {
			System.err.println("Confirming QoS reservation unsuccesful.");
			e.getCause();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tearPath() {
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "senderdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "pathtear", "");
		} catch (ClickException e) {
			System.err.println("tear path unsuccesful.");
			e.getCause();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void tearResv() {
		try {
			_controlSocket.write(_elementName, "session", "DEST " + dstIP + ", PROTOCOL 6, POLICE false, PORT " + dstPort);
			_controlSocket.write(_elementName, "flowdescriptor", "SRC_ADDRESS " + srcIP + ", SRC_PORT " + srcPort + ", TOKEN_BUCKET_RATE 5.3, TOKEN_BUCKET_SIZE 50.77, PEAK_DATA_RATE 2.6, MINIMUM_POLICED_UNIT 5, MAXIMUM_PACKET_SIZE 5");
			_controlSocket.write(_elementName, "resvtear", "");
		} catch (ClickException e) {
			System.err.println("tear resv unsuccesful.");
			e.getCause();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
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
	*/
}
