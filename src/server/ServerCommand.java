package server;


import model.Command;

class ServerCommand extends Command {
	private final ClientHandler sender;

	public ServerCommand(ClientHandler sender, String message) {
		super(message); // 親クラスのコンストラクタで解析
		this.sender = sender;
	}

	public ClientHandler getSender() {
		return sender;
	}
}