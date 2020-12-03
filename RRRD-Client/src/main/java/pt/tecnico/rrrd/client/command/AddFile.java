package pt.tecnico.rrrd.client.command;

public class AddFile implements ICommand {

    private String documentPath;

    public AddFile(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
