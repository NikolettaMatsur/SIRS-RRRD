package pt.tecnico.rrrd.client.command;

public class AddFile implements ICommand {

    private String documentPath;
    private String documentId;

    public AddFile(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentPath = commands[1];
        this.documentId = commands[2];
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
