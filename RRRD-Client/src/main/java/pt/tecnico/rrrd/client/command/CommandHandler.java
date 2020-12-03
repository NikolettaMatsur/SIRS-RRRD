package pt.tecnico.rrrd.client.command;

import pt.tecnico.rrrd.client.utils.Utils;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CommandHandler implements ICommandHandler {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public CommandHandler(RemoteServerBlockingStub blockingStub, RemoteServerStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    @Override
    public void handle(Pull pull) {
        try {
            PullMessage pullMessage = PullMessage.newBuilder().
                    setDocumentId(pull.getDocumentId()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            PullRequest pullRequest = PullRequest.newBuilder().
                    setMessage(pullMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", pullMessage.toByteArray())).
                    build();

            PullResponse pullResponse = this.blockingStub.pull(pullRequest);

            System.out.printf("Received response: Doc: %s; Key: %s;\n", pullResponse.getDocument(), pullResponse.getDocumentKey());

            // TODO decrypt document and write o File System
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(Push push) {
        try {
            String documentData = Files.readString(Paths.get(push.getDocumentPath()), StandardCharsets.US_ASCII);

            PushMessage pushMessage = PushMessage.newBuilder().
                    setDocumentId(push.getDocumentId()).
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument("password", "symmetric_key", "password", documentData.getBytes())).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            PushRequest pushRequest = PushRequest.newBuilder().
                    setMessage(pushMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", pushMessage.toByteArray())).
                    build();

            PushResponse pushResponse = this.blockingStub.push(pushRequest);

            System.out.printf("Received response: %s;\n", pushResponse.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(AddFile addFile) {
        try {
            String documentData = Files.readString(Paths.get(addFile.getDocumentPath()), StandardCharsets.US_ASCII);

            AddFileMessage addFileMessage = AddFileMessage.newBuilder().
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument("password", "symmetric_key", "password", documentData.getBytes())).
                    setOwnerUsername(Utils.getUserName()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            AddFileRequest addFileRequest = AddFileRequest.newBuilder().
                    setMessage(addFileMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", addFileMessage.toByteArray())).
                    build();

//            AddFileResponse addFileResponse = this.blockingStub.push(addFileRequest);
//            System.out.println("Received response: " + addFileResponse);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(AddPermission addPermission) {
        try {
            getPubKeys(addPermission.getUsername());

            AddPermissionMessage addPermissionMessage = AddPermissionMessage.newBuilder().
                    setDocumentId(addPermission.getDocumentId()).
                    addPubKeys("1").
                    addPubKeys("2").
                    addPubKeys("3").
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            AddPermissionRequest addPermissionRequest = AddPermissionRequest.newBuilder().
                    setMessage(addPermissionMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", addPermissionMessage.toByteArray())).
                    build();
            System.out.println(addPermissionRequest);
//            AddPermissionResponse addPermissionResponse = this.blockingStub.push(addPermissionRequest);
//            System.out.println("Received response: " + addPermissionResponse);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getPubKeys(String username) {
        try {
            GetPubKeysMessage getPubKeysMessage = GetPubKeysMessage.newBuilder().
                    setUsername(username).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            GetPubKeysRequest getPubKeysRequest = GetPubKeysRequest.newBuilder().
                    setMessage(getPubKeysMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", getPubKeysMessage.toByteArray())).
                    build();

            System.out.println(getPubKeysRequest);

//            GetPubKeysResponse getPubKeysResponse = this.blockingStub.push(getPubKeysRequest);
//            System.out.println("Received response: " + getPubKeysResponse);

            // TODO Encrypt document key with the received public keys
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
