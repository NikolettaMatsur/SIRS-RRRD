package pt.tecnico.rrrd.client.command;

import pt.tecnico.rrrd.client.utils.Utils;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import javax.crypto.SecretKey;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

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

            byte[] documentKeyBytes = CryptographicOperations.asymmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocumentKey()),
                    CryptographicOperations.getPrivateKey("password", "asymmetric_keys", "password"));

            SecretKey secretKey = CryptographicOperations.convertToSymmetricKey(documentKeyBytes);
            byte[] decryptedDocument = CryptographicOperations.symmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocument()),
                    secretKey);

            CryptographicOperations.storeDocumentKey("password", pull.getDocumentId(), secretKey);

            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pull.getOutputPath()), StandardCharsets.UTF_8));
            writer.write(new String(decryptedDocument));
        } catch (NoSuchFileException e) {
            System.out.println("No such file: " + e.getFile());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(Push push) {
        try {
            String documentData = Files.readString(Paths.get(push.getDocumentPath()), StandardCharsets.UTF_8);

            PushMessage pushMessage = PushMessage.newBuilder().
                    setDocumentId(push.getDocumentId()).
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument("password", push.getDocumentId(), documentData.getBytes())).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            PushRequest pushRequest = PushRequest.newBuilder().
                    setMessage(pushMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", pushMessage.toByteArray())).
                    build();

            PushResponse pushResponse = this.blockingStub.push(pushRequest);

            System.out.printf("Received response: %s;\n", pushResponse.getMessage());
        } catch (NoSuchFileException e) {
            System.out.println("No such file: " + e.getFile());
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
        }
    }

    @Override
    public void handle(AddFile addFile) {
        try {
            String documentData = Files.readString(Paths.get(addFile.getDocumentPath()), StandardCharsets.US_ASCII);

            SecretKey secretKey = CryptographicOperations.createDocumentKey();
            AddFileMessage addFileMessage = AddFileMessage.newBuilder().
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument("password", "symmetric_key", documentData.getBytes())).
                    setDocumentId(addFile.getDocumentId()).
                    setOwnerUsername(Utils.getUserName()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            AddFileRequest addFileRequest = AddFileRequest.newBuilder().
                    setMessage(addFileMessage).
                    setSignature(CryptographicOperations.getSignature("password", "asymmetric_keys", "password", addFileMessage.toByteArray())).
                    build();

//            AddFileResponse addFileResponse = this.blockingStub.push(addFileRequest);
//            System.out.println("Received response: " + addFileResponse);
//
//            if (addFileResponse.getOk()) {
//                CryptographicOperations.storeDocumentKey("password", addFile.getDocumentId(), secretKey);
//            }

        } catch (NoSuchFileException e) {
            System.out.println("No such file: " + e.getFile());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(AddPermission addPermission) {
        try {
            AddPermissionMessage.Builder builder = AddPermissionMessage.newBuilder().
                    setDocumentId(addPermission.getDocumentId()).
                    setTimestamp(CryptographicOperations.getTimestamp());

            for (String encryptedKey : getPubKeys(addPermission.getUsername(), addPermission.getDocumentId())) {
                builder.addPubKeys(encryptedKey);
            }

            AddPermissionMessage addPermissionMessage = builder.build();

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

    private List<String> getPubKeys(String username, String documentId) {
        List<String> keys = new LinkedList<>();
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
//
//            Key documentKey = CryptographicOperations.getDocumentKey("password", documentId);
//            for (String pubKey : getPubKeysResponse.getPubKeysList()) {
//                byte[] encryptedKey = CryptographicOperations.asymmetricEncrypt(documentKey.getEncoded(),
//                        CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(pubKey)));
//                keys.add(Base64.getEncoder().encodeToString(encryptedKey));
//            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return keys;
    }
}
