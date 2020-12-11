package pt.tecnico.rrrd.client.command;

import io.grpc.Status;
import pt.tecnico.rrrd.client.RrrdClientApp;
import pt.tecnico.rrrd.client.utils.Utils;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import javax.crypto.SecretKey;
import io.grpc.StatusRuntimeException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandHandler implements ICommandHandler {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;
    private final Logger logger;

    public CommandHandler(RemoteServerBlockingStub blockingStub, RemoteServerStub asyncStub) {
        this.logger = Logger.getLogger(CommandHandler.class.getName());
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
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", pullMessage.toByteArray())).
                    build();

            PullResponse pullResponse = this.blockingStub.pull(pullRequest);

            byte[] documentKeyBytes = CryptographicOperations.asymmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocumentKey()),
                    CryptographicOperations.getPrivateKey(RrrdClientApp.keyStorePassword, "asymmetric_keys"));

            SecretKey secretKey = CryptographicOperations.convertToSymmetricKey(documentKeyBytes);
            byte[] decryptedDocument = CryptographicOperations.symmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocument()),
                    secretKey);

            CryptographicOperations.storeDocumentKey(RrrdClientApp.keyStorePassword, pull.getDocumentId(), secretKey);

            PrintWriter writer = new PrintWriter("/home/" + Utils.getUserName() + "/sync/client/" + pull.getDocumentId() + ".txt", StandardCharsets.UTF_8);
            writer.println(new String(decryptedDocument));
            writer.close();
        } catch (NoSuchFileException e) {
            System.out.println("No such file: " + e.getFile());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DATA_LOSS) {
                System.err.println(e.getMessage());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(Push push) {
        try {

            String documentData = Files.readString(Paths.get("/home/" + Utils.getUserName() + "/sync/client/" + push.getDocumentId() + ".txt"), StandardCharsets.UTF_8);

            PushMessage pushMessage = PushMessage.newBuilder().
                    setDocumentId(push.getDocumentId()).
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument(RrrdClientApp.keyStorePassword, push.getDocumentId(), documentData.trim().getBytes())).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            PushRequest pushRequest = PushRequest.newBuilder().
                    setMessage(pushMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", pushMessage.toByteArray())).
                    build();

            logger.info(String.format("Sending Push Request: {Document Id: %s, Timestamp: %s}\n", pushMessage.getDocumentId(), pushMessage.getTimestamp()));

            PushResponse pushResponse = this.blockingStub.push(pushRequest);

            logger.info(String.format("Received Push Response: {Message: %s}\n", pushResponse.getMessage()));
        } catch (NoSuchFileException e) {
            logger.severe("No such file: " + e.getFile());
        } catch (Exception e) {
            logger.severe(e.getClass() + ": " + e.getMessage());
        }
    }

    @Override
    public void handle(AddFile addFile) {
        try {
            String documentData = Files.readString(Paths.get("/home/" + Utils.getUserName() + "/sync/client/" + addFile.getDocumentId() + ".txt"), StandardCharsets.US_ASCII);

            SecretKey secretKey = CryptographicOperations.createDocumentKey();
            AddFileMessage addFileMessage = AddFileMessage.newBuilder().
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument("password", "symmetric_key", documentData.getBytes())).
                    setDocumentId(addFile.getDocumentId()).
                    setOwnerUsername(Utils.getUserName()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            AddFileRequest addFileRequest = AddFileRequest.newBuilder().
                    setMessage(addFileMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", addFileMessage.toByteArray())).
                    build();

            AddFileResponse addFileResponse = this.blockingStub.addNewFile(addFileRequest); // TODO catch exception that is thrown when an incorrect documentId is specified
            System.out.println("Received response: " + addFileResponse);

            CryptographicOperations.storeDocumentKey("password", addFile.getDocumentId(), secretKey);

        } catch (NoSuchFileException e) {
            e.printStackTrace();
            System.out.println("No such file: " + e.getFile());
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            if (e.getStatus().getCode() == Status.Code.DATA_LOSS) {
                System.err.println(e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", addPermissionMessage.toByteArray())).
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
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", getPubKeysMessage.toByteArray())).
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

    @Override
    public boolean handle(Login login) {
        try {
            LoginCredentials loginCredentials = LoginCredentials.newBuilder().
                    setUsername(login.getUsername()).
                    setPassword(login.getPassword()).
                    build();

            String pubKey = Base64.getEncoder().encodeToString(CryptographicOperations.getPublicKey(RrrdClientApp.keyStorePassword, "asymmetric_keys").getEncoded());
            LoginMessage loginMessage = LoginMessage.newBuilder().
                    setCredentials(loginCredentials).
                    setClientPubKey(pubKey).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            LoginRequest loginRequest = LoginRequest.newBuilder().
                    setMessage(loginMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", loginMessage.toByteArray())).
                    build();

//            LoginResponse loginResponse = this.blockingStub.push(loginRequest);

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    @Override
    public void handle(Logout logout) {
        try {
//            LogoutResponse logoutResponse = this.blockingStub.push(LogoutRequest.newBuilder().build());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
