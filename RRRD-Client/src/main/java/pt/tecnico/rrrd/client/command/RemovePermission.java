package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public class RemovePermission implements ICommand {

    private String documentId;
    private String username;

    public RemovePermission(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
        this.username = commands[2];
    }

    public RemovePermission(String documentId, String username) {
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
    public void accept(ICommandHandler commandHandler) throws AuthenticationException {
        commandHandler.handle(this);
    }
}
