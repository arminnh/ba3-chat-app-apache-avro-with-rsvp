package server;
import java.util.HashMap;
import java.util.Map;

/*
 * We will first create the RemoteReferenceModule class on the server side. 
 * Its most important function is to keep track of the server objects 
 * that have been registered in the platform.
*/
public class RemoteReferenceModule {
	//remote object reference table
	Map<String, Object> remoteObjects = new HashMap<String, Object>();
	
	public String register(Object remoteObject) {
		String name = remoteObject.getClass().getSimpleName();
		
		if (remoteObjects.containsKey(name)) {
			// refuse to store more than one object of the same type, since we're using the singleton pattern
			return name;
		}
		
		remoteObjects.put(name, remoteObject);
		System.out.println("RemoteReferenceModule registered: " + name); 
		return name;
	}
	
	public Object retrieve(String remoteObjectName) {
		System.out.println("RemoteReferenceModule.retrieve: " + remoteObjectName);
		return remoteObjects.get(remoteObjectName);
	}
}