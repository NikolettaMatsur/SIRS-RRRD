package pt.tecnico.rrrd.client.command;

public class Pull implements ICommand {

    private String documentId;
    private String outputPath;

    public Pull(String documentId, String outputPath) {
        this.documentId = documentId;
        this.outputPath = outputPath;
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
