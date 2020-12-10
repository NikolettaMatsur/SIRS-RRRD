package pt.tecnico.rrrd.client.command;

public class Pull implements ICommand {

    private String documentId;

    public Pull(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
    }

    public Pull(String documentId, String outputPath) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
