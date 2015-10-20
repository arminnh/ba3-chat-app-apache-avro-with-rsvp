package client;
import server.*;

import java.lang.reflect.*;

import server.RemoteReferenceModule;

public class Hello {
	public static void main(String[] args) {
		Server server = new server.Server();
		System.out.println(server.sayHello("blablabla"));
		 
		RemoteReferenceModule remoote = new server.RemoteReferenceModule();
		System.out.println(remoote.register(new String("serhsgrhsgrsgrsgrsgrsgrhefio;eh")));
		System.out.println(remoote.register(new Integer(5)));
		remoote.register(10);
		System.out.println(remoote.retrieve("String"));
		System.out.println(remoote.retrieve("Integer"));
		
		

		Server myserver = new server.Server();
		Class<?> myserverClass = myserver.getClass();
		Method requestedMethod;
		Object result = new Object();
		try {
			requestedMethod = myserverClass.getMethod("sayHello", "josse".getClass());
			result = requestedMethod.invoke(myserver, "josse");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		System.out.println(result);
	}
	
	// String resultaat = obj.functie(a ,b)
	 
	
	// message(CLIENT, SERVER, "functie(a, b)")
	// String resultaat = Iets.stuurmessage(message)
}
