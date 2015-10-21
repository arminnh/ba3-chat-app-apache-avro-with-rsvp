package server;

public class SayHelloObject implements SayHelloObjectInterface {
	public String sayHello(String name) {
		return "Hello, " + name;
	}
	
	//complicated calculations
}