package pt.tecnico.rrrd.server.command;

public interface ICommand {

    void accept(ICommandHandler commandHandler);
}
