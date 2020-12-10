package pt.tecnico.rrrd.client.command;

public class Login implements ICommand {

    private String username;
    private String password;
    private boolean loggedIn;

    public Login(String username, String password) {
        this.username = username;
        this.password = password;
        this.loggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        this.loggedIn = commandHandler.handle(this);
    }
}
