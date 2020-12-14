package pt.tecnico.rrrd.client.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import pt.tecnico.rrrd.client.RrrdClientApp;
import pt.tecnico.rrrd.client.utils.Utils;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.management.InvalidAttributeValueException;
import javax.naming.AuthenticationException;

import io.grpc.StatusRuntimeException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandHandler implements ICommandHandler {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;
    private final Logger logger;
    private String clientRootDirectory = "/home/" + Utils.getUserName() + "/sync/client/";

    public CommandHandler(RemoteServerBlockingStub blockingStub, RemoteServerStub asyncStub) {
        this.logger = Logger.getLogger(CommandHandler.class.getName());
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    @Override
    public void handle(Pull pull) throws AuthenticationException {
        try {
            PullMessage pullMessage = PullMessage.newBuilder().
                    setDocumentId(pull.getDocumentId()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            PullRequest pullRequest = PullRequest.newBuilder().
                    setMessage(pullMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", pullMessage.toByteArray())).
                    build();

            logger.info(String.format("Pull Request sent for file %s", pull.getDocumentId()));
            PullResponse pullResponse = this.blockingStub.pull(pullRequest);
            logger.info(String.format("Pull Response received, decrypting...", pull.getDocumentId()));

            byte[] documentKeyBytes = CryptographicOperations.asymmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocumentKey()),
                    CryptographicOperations.getPrivateKey(RrrdClientApp.keyStorePassword, "asymmetric_keys"));

            SecretKey secretKey = CryptographicOperations.convertToSymmetricKey(documentKeyBytes);
            byte[] decryptedDocument = CryptographicOperations.symmetricDecrypt(Base64.getDecoder().decode(pullResponse.getDocument()),
                    secretKey);

            CryptographicOperations.storeDocumentKey(RrrdClientApp.keyStorePassword, pull.getDocumentId(), secretKey);
            String document = parseDocumentAndHash(decryptedDocument);

            PrintWriter writer = new PrintWriter( clientRootDirectory + pull.getDocumentId() + ".txt", StandardCharsets.UTF_8);
            writer.println(document);
            writer.close();
            logger.info(String.format("Pull Response decrypted and stored in %s.",clientRootDirectory + pull.getDocumentId() + ".txt"));

        } catch (NoSuchFileException e) {
            System.out.println("No such file: " + e.getFile());
        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(Push push) throws AuthenticationException {
        try {
            String documentData = Files.readString(Paths.get(clientRootDirectory + push.getDocumentId() + ".txt"), StandardCharsets.UTF_8);

            String jsonDocumentAndHash = createDocumentAndHash(documentData);

            PushMessage pushMessage = PushMessage.newBuilder().
                    setDocumentId(push.getDocumentId()).
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument(RrrdClientApp.keyStorePassword, push.getDocumentId(), jsonDocumentAndHash.getBytes())).
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
        } catch (StatusRuntimeException e) {
          verifyGRPCStatus(e);
        } catch (Exception e) {
            logger.severe(e.getClass() + ": " + e.getMessage());
        }
    }

    @Override
    public void handle(AddFile addFile) throws AuthenticationException {
        try {
            String documentData = Files.readString(Paths.get(clientRootDirectory + addFile.getDocumentId() + ".txt"), StandardCharsets.US_ASCII);
            String jsonDocumentAndHash = createDocumentAndHash(documentData);

            SecretKey secretKey = CryptographicOperations.createDocumentKey();
            AddFileMessage addFileMessage = AddFileMessage.newBuilder().
                    setEncryptedDocument(CryptographicOperations.getEncryptedDocument(secretKey, jsonDocumentAndHash.getBytes())).
                    setDocumentId(addFile.getDocumentId()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            AddFileRequest addFileRequest = AddFileRequest.newBuilder().
                    setMessage(addFileMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", addFileMessage.toByteArray())).
                    build();

            AddFileResponse addFileResponse = this.blockingStub.addNewFile(addFileRequest); // TODO catch exception that is thrown when an incorrect documentId is specified
            System.out.println("Received response: " + addFileResponse);
            CryptographicOperations.storeDocumentKey(RrrdClientApp.keyStorePassword, addFile.getDocumentId(), secretKey);

            this.handle(new AddPermission(addFile.getDocumentId(), RrrdClientApp.username));

        } catch (NoSuchFileException e) {
//            e.printStackTrace();
            System.out.println("No such file: " + e.getFile());
        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(AddPermission addPermission) throws AuthenticationException {
        try {
            AddPermissionMessage addPermissionMessage = AddPermissionMessage.newBuilder().
                    setDocumentId(addPermission.getDocumentId()).
                    setUsername(addPermission.getUsername()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    putAllPubKeys(getPubKeys(addPermission.getUsername(), addPermission.getDocumentId())).
                    build();

            AddPermissionRequest addPermissionRequest = AddPermissionRequest.newBuilder().
                    setMessage(addPermissionMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", addPermissionMessage.toByteArray())).
                    build();

            AddPermissionResponse addPermissionResponse = this.blockingStub.addPermission(addPermissionRequest);
            System.out.println("Received response: " + addPermissionResponse.getMessage());
        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private Map<Integer, String> getPubKeys(String username, String documentId) throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, SignatureException, InvalidKeyException, InvalidKeySpecException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {

        GetPubKeysMessage getPubKeysMessage = GetPubKeysMessage.newBuilder().
                setUsername(username).
                setTimestamp(CryptographicOperations.getTimestamp()).
                build();

        GetPubKeysRequest getPubKeysRequest = GetPubKeysRequest.newBuilder().
                setMessage(getPubKeysMessage).
                setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", getPubKeysMessage.toByteArray())).
                build();

        GetPubKeysResponse getPubKeysResponse = this.blockingStub.getPubKeys(getPubKeysRequest);

        Key documentKey = CryptographicOperations.getDocumentKey(RrrdClientApp.keyStorePassword, documentId);
        Map<Integer, String> pubKeysTemp = getPubKeysResponse.getPubKeysMap();
        HashMap<Integer, String> pubKeys = new HashMap<>(pubKeysTemp);

        for (Map.Entry<Integer, String> pubKey : pubKeys.entrySet()) {
            byte[] encryptedKey =  CryptographicOperations.asymmetricEncrypt(documentKey.getEncoded(),
                    CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(pubKey.getValue())));

            pubKeys.replace(pubKey.getKey(),Base64.getEncoder().encodeToString(encryptedKey));
        }

        return pubKeys;
    }

    @Override
    public String handle(Login login) {
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

            LoginResponse loginResponse = this.blockingStub.login(loginRequest);

            return loginResponse.getToken();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public void handle(Logout logout) throws AuthenticationException {
        try {
            LogoutResponse logoutResponse = this.blockingStub.logout(LogoutRequest.newBuilder().build());
        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(DeleteFile deleteFile) throws AuthenticationException {
        try {
            DeleteMessage deleteMessage = DeleteMessage.newBuilder().
                    setDocumentId(deleteFile.getDocumentId()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            DeleteFileRequest deleteFileRequest = DeleteFileRequest.newBuilder().
                    setMessage(deleteMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", deleteMessage.toByteArray())).
                    build();
            logger.info("Delete file request sent");

            this.blockingStub.deleteFile(deleteFileRequest);

            logger.info("Delete file done.");

        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(RemovePermission removePermission) throws AuthenticationException {
        try {
            logger.info("Remove permission request sent");

            RemovePermissionMessage removePermissionMessage = RemovePermissionMessage.newBuilder().
                    setDocumentId(removePermission.getDocumentId()).
                    setUsername(removePermission.getUsername()).
                    setTimestamp(CryptographicOperations.getTimestamp()).
                    build();

            RemovePermissionRequest removePermissionRequest = RemovePermissionRequest.newBuilder().
                    setMessage(removePermissionMessage).
                    setSignature(CryptographicOperations.getSignature(RrrdClientApp.keyStorePassword, "asymmetric_keys", removePermissionMessage.toByteArray())).
                    build();

            this.blockingStub.removePermission(removePermissionRequest);
            logger.info("Remove permission donne");
        } catch (StatusRuntimeException e) {
            verifyGRPCStatus(e);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handle(PrintPubKeys printPubKeys) throws AuthenticationException {
        try {
            System.out.println("User public key is: " + Base64.getEncoder().encodeToString(CryptographicOperations.getPublicKey(RrrdClientApp.keyStorePassword, "asymmetric_keys").getEncoded()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private String createDocumentAndHash(String documentData) throws NoSuchAlgorithmException {
        JsonObject documentAndHash = new JsonObject();
        documentAndHash.addProperty("documentData", documentData);
        documentAndHash.addProperty("hash", CryptographicOperations.createMessageDigest(documentData));

        return documentAndHash.toString();
    }

    private String parseDocumentAndHash(byte[] data) throws NoSuchAlgorithmException, InvalidAttributeValueException {
        String json = new String(data);

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        if (!CryptographicOperations.verifyMessageDigest(jsonObject.get("documentData").getAsString(), jsonObject.get("hash").getAsString())) {
            throw new InvalidAttributeValueException("Document integrity could not be verified");
        }

        return jsonObject.get("documentData").getAsString();
    }

    void verifyGRPCStatus(StatusRuntimeException e) throws AuthenticationException {
        String message = "";
        if (e.getStatus().getCode() == Status.Code.DATA_LOSS) {
            if("test".equals("add_file")){
                message = "File already exists.";
            }
            logger.severe(String.valueOf(e.getStatus().getCode())+ ": "+ message);
        }
        else if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
            throw new AuthenticationException("Token expired please login again.");
        }
        else if (e.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {
            logger.severe("User does not have permission for that file.");
        }
        else if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
            logger.severe("Resource does not exist.");
        }else {
            logger.severe(String.valueOf(e.getStatus().getCode()));
        }
    }

    public void changeRootDirectory(String path){
        this.clientRootDirectory = path;
    }
}
