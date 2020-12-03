package pt.tecnico.rrrd.client.command;

public interface ICommand {

    void accept(ICommandHandler commandHandler);
}
