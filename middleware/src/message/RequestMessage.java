package message;

public class RequestMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String methodName;
	public Class<?>[] paramTypes;
	public Object[] paramValues;
	
	public RequestMessage(String from, String to) {
		super(from, to);
	}
	
	public RequestMessage(String from, String to, String methodName, Class<?>[] paramTypes, Object[] paramValues) {
		super(from, to);
		this.methodName = methodName;
		this.paramTypes = paramTypes;
		this.paramValues = paramValues;
	}
	
}
