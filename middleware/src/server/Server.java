package server;

public class Server implements ServerInterface {
	public String sayHello(String name) {
		return "Hello, " + name;
	}
}