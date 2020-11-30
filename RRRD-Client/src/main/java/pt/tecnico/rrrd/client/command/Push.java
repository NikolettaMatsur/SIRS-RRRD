package pt.tecnico.rrrd.client.command;

public class Push implements ICommand {

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
