package client;

import java.lang.reflect.*;

public class ProxyLookup {
	public static Object lookup(Class<?> cl, CommunicationModule communicationModule) {
		
		MiddlewareInvocationHandler handler = communicationModule.invocationHandler();
		communicationModule.setClass(cl);
		
		Object proxy = Proxy.newProxyInstance(cl.getClassLoader(), new Class<?>[] { cl.getInterfaces()[0] }, handler);
		
		return proxy;
	}
}
