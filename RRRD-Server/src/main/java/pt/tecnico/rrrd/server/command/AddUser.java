package pt.tecnico.rrrd.server.command;

public class AddUser implements ICommand{

    private String username;

    private String password;

    public AddUser(String commandInput) {
        String[] commands = commandInput.split(" ");
        this.username = commands[0];
        this.password = commands[1];
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
