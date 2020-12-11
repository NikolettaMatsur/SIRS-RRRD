package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public class Push implements ICommand {

    private String documentId;

    public Push(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
    }

    public Push(String documentId, String documentPath) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) throws AuthenticationException {
        commandHandler.handle(this);
    }
}
