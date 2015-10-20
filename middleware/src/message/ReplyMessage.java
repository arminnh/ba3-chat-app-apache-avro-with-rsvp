package message;

public class ReplyMessage extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Object object;
	
	public ReplyMessage(String from, String to) {
		super(from, to);
	}
	
	public ReplyMessage(String from, String to, Object object) {
		super(from, to);
		this.object = object;
	}
	
}
