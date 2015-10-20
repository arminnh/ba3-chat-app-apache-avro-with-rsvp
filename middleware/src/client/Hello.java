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

		/*
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
		*/
		
		Server testserver = new server.Server();
		DispatchingModule test = new server.DispatchingModule();
		Class<?>[] paramType = {"Josse".getClass()};
		Object[] paramValue = {"Josse"};
		message.RequestMessage testmessage = new message.RequestMessage("a", "b", "sayHello", paramType, paramValue);
		//server.sayHello("Josse")
		message.ReplyMessage reply = test.dispatchCall(testmessage, testserver);
		System.out.println(reply.object);
		
	}

	// String resultaat = obj.functie(a ,b)


	// message(CLIENT, SERVER, "functie(a, b)")
	// String resultaat = Iets.stuurmessage(message)
}


//communication module
/*
* DispatchingModule dispatch = new DispatchingModule();
* Object blabla = dispatch(requestMessage, object);
* return message.ReplyMessage("from", "to", result.getClass(), result);
*/

