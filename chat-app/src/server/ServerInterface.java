package server;


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
