package server;

public class Request {
	private String from, to;
	private RequestStatus status;
	
	public Request(String from, String to, RequestStatus status) {
		super();
		this.from = from;
		this.to = to;
		this.status = status;
	}
	
	public RequestStatus getStatus() {
		return status;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	@Override
	public String toString() {
		return "Request [from=" + from + ", to=" + to + "]";
	}
}
