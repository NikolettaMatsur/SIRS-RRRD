package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public interface ICommandHandler {

    void handle(Pull pull) throws AuthenticationException;

    void handle(Push push) throws AuthenticationException;

    void handle(AddFile addFile) throws AuthenticationException;

    void handle(AddPermission addPermission) throws AuthenticationException;

    String handle(Login login);

    void handle(Logout logout) throws AuthenticationException;

    void handle(DeleteFile deleteFile) throws AuthenticationException;
}
