package message;

/*
 * The request message should carry all necessary information 
 * to describe a method call by the client on a remote object. 
 * The response message should carry the information that is returned by the method call.
 */
public class Message implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String from, to;
	
	public Message(String from, String to) {
		this.from = from;
		this.to = to;
	}
}
