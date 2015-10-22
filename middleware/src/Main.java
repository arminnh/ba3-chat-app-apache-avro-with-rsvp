
public class Main {
	public static void main(String[] args) {
		if (args[0].equals("client")) {
			 client.Client.main(new String[] {args[1], args[2]});
		} else if (args[0].equals("server")) {
			server.Server.main(new String[] {args[1]});
		}
	}
}
