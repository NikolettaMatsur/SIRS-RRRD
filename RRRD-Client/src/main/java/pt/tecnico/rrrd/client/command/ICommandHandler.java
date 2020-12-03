package pt.tecnico.rrrd.client.command;

public interface ICommandHandler {

    void handle(Pull pull);

    void handle(Push push);

    void handle(AddFile addFile);

    void handle(AddPermission addPermission);
}
