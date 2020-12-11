package pt.tecnico.rrrd.server;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.server.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class RrrdServerService extends RemoteServerGrpc.RemoteServerImplBase {
    private final Logger logger;
    private DatabaseManager databaseManager;

    public RrrdServerService() throws IOException, ClassNotFoundException {
        this.logger = Logger.getLogger(RrrdServerApp.class.getName());
        this.databaseManager = new DatabaseManager();
    }

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        try {
            logger.info(String.format("Received Pull Request: {Document Id: %s, Timestamp: %s}\n", request.getMessage().getDocumentId(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));

            // TODO verify user permissions

            if (verifySig && verifyTimestamp) {
                logger.info(String.format("Signature and Timestamp verified. Returning encrypted file: %s", Utils.getFileRepository(request.getMessage().getDocumentId())));

                String key = ""; // TODO get file key encrypted with users public key

                String encryptedDocumentData = Files.readString(Paths.get(Utils.getFileRepository(request.getMessage().getDocumentId())), StandardCharsets.UTF_8);

                PullResponse pullResponse = PullResponse.newBuilder().
                        setDocument(encryptedDocumentData.trim()).
                        setDocumentKey(key).
                        build();

                responseObserver.onNext(pullResponse);
                responseObserver.onCompleted();
            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }

        } catch (Exception e) {
            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void push(PushRequest request, StreamObserver<PushResponse> responseObserver) {
        try {
            PushMessage pushMessage = request.getMessage();
            byte[] signature = Base64.getDecoder().decode(request.getSignature());
            PublicKey publicKey;


            logger.info(String.format("Received Push Request: {Document Id: %s, Timestamp: %s}\n", pushMessage.getDocumentId(), pushMessage.getTimestamp()));

            publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, pushMessage.toByteArray(), signature);
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(pushMessage.getTimestamp());
            if (!verifySig || !verifyTimestamp) {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.severe(message + " Aborting operation.");
                PushResponse pushResponse = PushResponse.newBuilder().setMessage(message).build();
                responseObserver.onNext(pushResponse);
                responseObserver.onCompleted();
            } else {
                logger.info(String.format("Signature and Timestamp verified. Writing encrypted file: %s", Utils.getFileRepository(pushMessage.getDocumentId())));

                PrintWriter writer = new PrintWriter(Utils.getFileRepository(pushMessage.getDocumentId()), StandardCharsets.UTF_8);
                writer.println(pushMessage.getEncryptedDocument());
                writer.close();

                PushResponse pushResponse = PushResponse.newBuilder().setMessage("OK").build();

                responseObserver.onNext(pushResponse);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            PushResponse pushResponse = PushResponse.newBuilder().setMessage(e.getMessage()).build();
            responseObserver.onNext(pushResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void addNewFile(AddFileRequest request, StreamObserver<AddFileResponse> responseObserver) {
        try {
            logger.info(String.format("Received AddNewFile Request: {Document Id: %s, Timestamp: %s}\n", request.getMessage().getDocumentId(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());
            boolean fileExits = new File(Utils.getFileRepository(request.getMessage().getDocumentId())).isFile();

            if (verifySig && verifyTimestamp && !fileExits) {
                logger.info(String.format("Signature and Timestamp verified. Writing new file: %s", Utils.getFileRepository(request.getMessage().getDocumentId())));

                // TODO add username to db and associate as the owner

                PrintWriter writer = new PrintWriter(Utils.getFileRepository(request.getMessage().getDocumentId()), StandardCharsets.UTF_8);
                writer.println(request.getMessage().getEncryptedDocument());
                writer.close();

                AddFileResponse addFileResponse = AddFileResponse.newBuilder().
                        setMessage("OK").
                        build();

                responseObserver.onNext(addFileResponse);
                responseObserver.onCompleted();
            } else {
                String message = !verifySig ? "Invalid Signature." : fileExits ? "File Already Exists" : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }

        } catch (Exception e) {
            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPubKeys(GetPubKeysRequest request, StreamObserver<GetPubKeysResponse> responseObserver) {
        try {
            logger.info(String.format("Received GetPubKeys Request: {Username: %s, Timestamp: %s}\n", request.getMessage().getUsername(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");

                // TODO get all public keys from request.getMessage().getUsername()
                List<String> pubKeys = new LinkedList<>();

                GetPubKeysResponse getPubKeysResponse = GetPubKeysResponse.newBuilder().
                        addAllPubKeys(pubKeys).
                        build();

                responseObserver.onNext(getPubKeysResponse);
                responseObserver.onCompleted();
            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }

        } catch (Exception e) {
            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void addPermission(AddPermissionRequest request, StreamObserver<AddPermissionResponse> responseObserver) {
        try {
            logger.info(String.format("Received AddPermission Request: {Username: %s, Timestamp: %s}\n", request.getMessage().getUsername(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.getPublicKey("password", "asymmetric_keys"); // TODO should be the users public key
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");

                // TODO verify if document request.getMessage().getDocumentId() exists
                // TODO verify if logged user id the owner of the document request.getMessage().getDocumentId()
                // TODO verify if user request.getMessage().getUsername() exists
                // TODO store request.getMessage().getPubKeys()
                // TODO give permission to user request.getMessage().getUsername() on file request.getMessage().getDocumentId()

                AddPermissionResponse addPermissionResponse = AddPermissionResponse.newBuilder().setMessage("OK").build();

            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }

        } catch (Exception e) {
            responseObserver.onError(Status.DATA_LOSS
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}
