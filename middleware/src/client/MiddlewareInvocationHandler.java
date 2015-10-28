package client;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MiddlewareInvocationHandler implements InvocationHandler {

	private CommunicationModule communicationModule;
	private Class<?> cl;
	
	MiddlewareInvocationHandler(Class<?> cl, CommunicationModule communicationModule) {
		this.communicationModule = communicationModule;
		this.cl = cl;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
		return this.communicationModule.remoteInvocation(proxy, method, args);
	}

	public Class<?> cl() {
		return this.cl;
	}
	
}
