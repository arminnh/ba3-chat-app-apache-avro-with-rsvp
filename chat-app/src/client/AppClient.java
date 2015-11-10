package client;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import chat_app.AppServerInterface;
import chat_app.AppClientInterface;

/*
 * - automatische registratie bij de server
 * - lijst opvragen van alle online gebruikers
 * - gebruiker kan publieke chatroom joinen
 * - requests voor prive gesprekken sturen, accepteren, weigeren
 *   (in elke situatie: publieke chatroom, prive gesprek)
 * - video streaming
 * - maximaal 1 conversatie tegelijk
 */

public class AppClient implements AppClientInterface {

	@Override
	public int recieveMessage(CharSequence message) throws AvroRemoteException {
		System.out.println(message);
		return 0;
	}
	
	public static void main(String[] argv) {
		try {
			System.out.println("Client create proxy.");
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(6789));
			AppServerInterface proxy = (AppServerInterface) SpecificRequestor.getClient(AppServerInterface.class,  client);
			int id = proxy.registerClient("armin", "0.0.0.0", 6789);
			System.out.println("Recieved id: " + id);
			proxy.exitClient(id);
			client.close();
			System.out.println("Client exit program.");
		} catch (IOException e) {
			System.err.println("Error connecting to server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
// Runtime.getRuntime().addShutdownHook