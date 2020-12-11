package pt.tecnico.rrrd.client.command;

public class Login implements ICommand {

    private String username;
    private String password;
    private boolean loggedIn;
    private String token;

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

    public String getToken() {
        return token;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        String token = commandHandler.handle(this);

        if (token != null) {
            this.loggedIn = true;
            this.token = token;
        } else {
            this.loggedIn = false;
        }
    }
}
