package pt.tecnico.rrrd.server.command;

public class UpdateUserPassword implements ICommand{

    private String username;

    private String newPassword;

    public UpdateUserPassword(String commandInput) {
        String[] commands = commandInput.split(" ");
        this.username = commands[0];
        this.newPassword = commands[1];
        //the old password is not asked as this is a sys admin function
    }

    public String getUsername() {
        return username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}

