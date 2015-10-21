package client;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MiddlewareInvocationHandler implements InvocationHandler {

	private CommunicationModule communicationModule;
	
	MiddlewareInvocationHandler(CommunicationModule communicationModule) {
		this.communicationModule = communicationModule;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	
		return this.communicationModule.remoteInvocation(proxy, method, args);
	}

}
