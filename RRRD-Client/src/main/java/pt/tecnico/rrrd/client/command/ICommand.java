package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public interface ICommand {

    void accept(ICommandHandler commandHandler) throws AuthenticationException;
}
