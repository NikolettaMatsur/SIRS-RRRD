package pt.tecnico.rrrd.server.command;

public class DeletePubKey implements ICommand{
    private String username;

    private Integer pubKeyId;

    public DeletePubKey(String commandInput) {
        String[] commands = commandInput.split(" ");
        this.username = commands[0];
        this.pubKeyId = Integer.parseInt(commands[1]);
    }

    public String getUsername() {
        return username;
    }

    public Integer getPubKeyId() {
        return pubKeyId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
