package pt.tecnico.rrrd.client.command;

public class Pull implements ICommand {

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
