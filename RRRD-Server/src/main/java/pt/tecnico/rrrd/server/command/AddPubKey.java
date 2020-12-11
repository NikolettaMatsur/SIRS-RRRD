package pt.tecnico.rrrd.server.command;

public class AddPubKey implements ICommand{

    private String username;

    private String pubKey;

    public AddPubKey(String commandInput) {
        String[] commands = commandInput.split(" ");
        this.username = commands[0];
        this.pubKey = commands[1];
    }

    public String getUsername() {
        return username;
    }

    public String getPubKey() {
        return pubKey;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
