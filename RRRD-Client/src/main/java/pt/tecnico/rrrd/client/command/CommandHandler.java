package pt.tecnico.rrrd.client.command;

import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.PrivateKey;
import java.util.Base64;

public class CommandHandler implements ICommandHandler {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public CommandHandler(RemoteServerBlockingStub blockingStub, RemoteServerStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    private String getSignature(String keyStorePassword, String privateKeyPassword, byte[] data) {
        byte[] signature = null;
        try {
            PrivateKey privateKey = CryptographicOperations.getPrivateKey(keyStorePassword, "asymmetric_keys", privateKeyPassword);
            signature = CryptographicOperations.sign(data, privateKey);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return Base64.getEncoder().encodeToString(signature);
    }

    private String getEncryptedDocument(String keyStorePassword, String documentPassword, String path) {
        byte[] encryptedDocument = null;
        try {
            String documentData = Files.readString(Paths.get(path), StandardCharsets.US_ASCII);
            System.out.println(">>>" + documentData);
            Key key = CryptographicOperations.getDocumentKey(keyStorePassword, "symmetric_key", documentPassword);
            encryptedDocument = CryptographicOperations.symmetricEncrypt(documentData.getBytes(), key);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return Base64.getEncoder().encodeToString(encryptedDocument);
    }

    @Override
    public void handle(Pull pull) {
        PullMessage pullMessage = PullMessage.newBuilder().
                setDocumentId(pull.getDocumentId()).
                setTimestamp(CryptographicOperations.getTimestamp()).
                build();

        PullRequest pullRequest = PullRequest.newBuilder().
                setMessage(pullMessage).
                setSignature(getSignature("password", "password", pullMessage.toByteArray())).
                build();

        PullResponse pullResponse = this.blockingStub.pull(pullRequest);
        System.out.printf("Received response: Doc: %s; Key: %s;\n", pullResponse.getDocument(), pullResponse.getDocumentKey());
        // TODO decrypt document and write o FS
    }

    @Override
    public void handle(Push push) {
        PushMessage pushMessage = PushMessage.newBuilder().
                setDocumentId(push.getDocumentId()).
                setEncryptedDocument(getEncryptedDocument("password", "password", push.getDocumentPath())).
                setTimestamp(CryptographicOperations.getTimestamp()).
                build();

        PushRequest pushRequest = PushRequest.newBuilder().
                setMessage(pushMessage).
                setSignature(getSignature("password", "password", pushMessage.toByteArray())).
                build();

        PushResponse pushResponse = this.blockingStub.push(pushRequest);

        System.out.printf("Received response: %s;\n", pushResponse.getMessage());
    }
}
