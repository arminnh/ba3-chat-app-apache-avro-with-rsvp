package server;

public class SayHelloObject implements interfaces.SayHelloObjectInterface {
	public String sayHello(String name) {
		return "Hello, " + name;
	}
	
	public Integer add(Integer a, Integer b) {
		return a + b;
	}
	
	//complicated calculations
}