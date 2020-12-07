package pt.tecnico.rrrd.client.command;

public class AddFile implements ICommand {

    private String documentPath;

    public AddFile(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentPath = commands[1];
    }

    public String getDocumentPath() {
        return documentPath;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
