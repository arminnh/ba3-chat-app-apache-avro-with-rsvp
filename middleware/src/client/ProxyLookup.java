package client;

import java.lang.reflect.*;

public class ProxyLookup {
	public static Object lookup(Class<?> cl, CommunicationModule communicationModule) {
		
		MiddlewareInvocationHandler handler = communicationModule.invocationHandler();
		Object proxy = Proxy.newProxyInstance(cl.getClassLoader(), new Class<?>[] { cl }, handler);
		
		return proxy;
	}
}
