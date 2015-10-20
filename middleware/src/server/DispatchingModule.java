package server;
import message.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* 
 * It allows clients to invoke methods of remote objects by identifying 
 * the appropriate method in the remote object interface and passing the request message to the object
 */
public class DispatchingModule {
	
	public Object dispatchCall(RequestMessage request, Object object) {
		Class<?> objectClass = request.getClass();
		Method requestedMethod;
		Object result = new Object();
		try {
			requestedMethod = objectClass.getMethod(request.methodName, request.paramTypes);
			result = requestedMethod.invoke(object, request.paramValues);
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
		
		//return result;
		return new ReplyMessage(request.to, request.from, result);
	}	
}

// communication module
/*
 * DispatchingModule dispatch = new DispatchingModule();
 * Object blabla = dispatch(requestMessage, object);
 * return message.ReplyMessage("from", "to", result.getClass(), result);
 */
