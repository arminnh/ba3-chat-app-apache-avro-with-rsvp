package errorwriter;

public class ErrorWriter {
	public static String getErrorMessage(int errorCode) {
		String error = "";

		switch (errorCode) {
			case 1:
				error = "Client was disconnected.";
				break;
			case 2:
				error = "Client could not be unregistered.";
				break;
			case 3:
				error = "That user does not exist.";
				break;
			case 4:
				error = "That request already exists.";
				break;
			case 5:
				error = "That request does not exist.";
				break;
			case 6:
				error = "Request could not be found";
				break;
			case 7:
				error = "That user is busy right now.";
				break;
			case 8:
				error = "Could not send the image.";
				break;
			case 9:
				error = "Could not register user.";
				break;
			case 10:
				error = "Error in chat mode.";
				break;
			case 11:
				error = "You cannot start a private chat without an accepted request.";
				break;
			case 12:
				error = "";
				break;
			default:
				error = "Invalid error code";
				break;
		}

		return error;
	}

	public static void printError(int errorCode) {
		System.err.println("Error code " + errorCode + ": " + getErrorMessage(errorCode));
	}
}
