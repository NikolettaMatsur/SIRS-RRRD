package pt.tecnico.rrrd.client.command;

import javax.naming.AuthenticationException;

public class DeleteFile implements ICommand {

    private String documentId;

    public DeleteFile(String commandInput) {
        String[] commands = commandInput.split(" ");

        this.documentId = commands[1];
    }

    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void accept(ICommandHandler commandHandler) throws AuthenticationException {
        commandHandler.handle(this);
    }
}
