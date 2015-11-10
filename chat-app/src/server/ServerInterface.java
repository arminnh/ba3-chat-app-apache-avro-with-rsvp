package server;

/*
 * We verwachten dat je volgende features hebt ge ̈ımplementeerd en kan demonstreren:
 * 	Een client moet kunnen registreren bij de server en de applicatie correct kunnen verlaten.
 * 	Een client moet een lijst kunnen opvragen van de verbonden clients.
 * 	Een client moet de publieke chat room kunnen joinen en daarin boodschappen versturen.
*/

public interface ServerInterface {
	/*
	 * status = enum{lobby, public chat, private chat}
	 * connectedUsers = {username, inetSocketAddress, state}
	 * 
	 * registerClient //zet client in connected users lijst
	 * exitClient     //remove client van de lijst
	 * 
	 * getListOfClients //return lijst van connected user
	 * 
	 * joinPublicChat // client state = public chat
	 * sendMessage    // while client state = public chat: send messages to public chat
	 * exitPublicChat // client state = lobby
	 */

}
