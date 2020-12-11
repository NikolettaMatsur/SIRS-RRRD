package pt.tecnico.rrrd.server.command;

public class DeleteUser implements ICommand{
    private String username;

    public DeleteUser(String commandInput) {
        String[] commands = commandInput.split(" ");
        this.username = commands[1];
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
