package pt.tecnico.rrrd.client.communication;

import pt.tecnico.rrrd.client.communication.CryptographicOperations;
import pt.tecnico.rrrd.client.PullResponse;

public class MessageHandler {

    private final CryptographicOperations cryptographicOperations;

    public MessageHandler(CryptographicOperations cryptographicOperations) {
        this.cryptographicOperations = cryptographicOperations;
    }

    public String getDocument(PullResponse pullResponse) {
        return this.cryptographicOperations.getDocument(pullResponse);
    }
}
