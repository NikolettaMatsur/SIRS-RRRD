package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public class Logout implements ICommand {

    @Override
    public void accept(ICommandHandler commandHandler) throws AuthenticationException {
        commandHandler.handle(this);
    }
}
