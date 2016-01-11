package client;

import java.lang.reflect.*;

public class ProxyLookup {
	public static Object lookup(Class<?> cl, CommunicationModule communicationModule) {
		
		Object proxy = Proxy.newProxyInstance(cl.getClassLoader(), new Class<?>[] { cl }, communicationModule);
		
		return proxy;
	}
}
