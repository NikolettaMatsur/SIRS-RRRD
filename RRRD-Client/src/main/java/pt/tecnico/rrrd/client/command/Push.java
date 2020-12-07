package pt.tecnico.rrrd.client.command;

public class Push implements ICommand {

    private String documentId;
    private String documentPath;

    public Push(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
        this.documentPath = commands[2];
    }

    public Push(String documentId, String documentPath) {
        this.documentId = documentId;
        this.documentPath = documentPath;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
