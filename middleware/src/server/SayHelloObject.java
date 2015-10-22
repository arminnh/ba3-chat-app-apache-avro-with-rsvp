package server;

public class SayHelloObject implements SayHelloObjectInterface {
	public String sayHello(String name) {
		return "Hello, " + name;
	}
	
	public Integer add(Integer a, Integer b) {
		return a + 2 * b;
	}
	
	//complicated calculations
}