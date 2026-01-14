package server;


import model.CommandType;

class Command {
	private final ClientHandler sender;
	private final CommandType commandType;
	private final String commandBody;

	public Command(final ClientHandler sender, final String message) {
		this.sender = sender;
		int index = message.indexOf(":");
		CommandType type;
		String body;
		try {
			if (index == -1) {
				type = CommandType.fromId(Integer.parseInt(message));
				body = "";
			} else {
				type = CommandType.fromId(Integer.parseInt(message.substring(0, index)));
				body = message.substring(index + 1);
			}
		} catch (NumberFormatException e) {
			type = CommandType.UNKNOWN;
			body = "";
		}
		commandType = type;
		commandBody = body;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public ClientHandler getSender() {
		return sender;
	}

	public String getBody() {
		return commandBody;
	}
}
