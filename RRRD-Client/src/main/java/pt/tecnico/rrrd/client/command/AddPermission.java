package pt.tecnico.rrrd.client.command;

public class AddPermission implements ICommand {

    private String documentId;
    private String username;

    public AddPermission(String documentId, String username) {
        this.documentId = documentId;
        this.username = username;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void accept(ICommandHandler commandHandler) {
        commandHandler.handle(this);
    }
}
