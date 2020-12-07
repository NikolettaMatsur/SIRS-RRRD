package pt.tecnico.rrrd.client.command;

public class Pull implements ICommand {

    private String documentId;
    private String outputPath;

    public Pull(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
        this.outputPath = commands[2];
    }

    public Pull(String documentId, String outputPath) {
        this.documentId = documentId;
        this.outputPath = outputPath;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getOutputPath() {
        return outputPath;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
