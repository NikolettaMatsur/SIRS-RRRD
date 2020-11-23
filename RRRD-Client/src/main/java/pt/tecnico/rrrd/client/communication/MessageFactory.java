package pt.tecnico.rrrd.client.communication;

import pt.tecnico.rrrd.client.PullRequest;
import pt.tecnico.rrrd.client.PullMessage;
import pt.tecnico.rrrd.client.PushMessage;
import pt.tecnico.rrrd.client.PushRequest;
import pt.tecnico.rrrd.client.communication.CryptographicOperations;

import com.googlecode.protobuf.format.JsonFormat;

public class MessageFactory {

    private final CryptographicOperations cryptographicOperations;
    private JsonFormat jsonFormat;

    public MessageFactory(CryptographicOperations cryptographicOperations) {
        this.cryptographicOperations = cryptographicOperations;
        this.jsonFormat = new JsonFormat();
    }

    public PullRequest createPullRequestMessage(String documentId) {
        PullMessage pullMessage = PullMessage.newBuilder().setDocumentId(documentId).setTimestamp(this.cryptographicOperations.getTimestamp()).build();
        String jsonPullMessage = this.jsonFormat.printToString(pullMessage);

        return PullRequest.newBuilder().setMessage(pullMessage).setSignature(this.cryptographicOperations.sign(jsonPullMessage)).build();
    }

    public PushRequest createPushRequestMessage(String documentId, String document) {
        String base64EncryptedDocument = this.cryptographicOperations.encryptDocument(document);

        PushMessage pushMessage = PushMessage.newBuilder().setDocumentId(documentId).setEncryptedDocument(base64EncryptedDocument)
                .setTimestamp(this.cryptographicOperations.getTimestamp()).build();
        String jsonPushMessage = this.jsonFormat.printToString(pushMessage);

        return PushRequest.newBuilder().setMessage(pushMessage).setSignature(this.cryptographicOperations.sign(jsonPushMessage)).build();
    }


}