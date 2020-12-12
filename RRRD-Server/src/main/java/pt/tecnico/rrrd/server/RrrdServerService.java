package pt.tecnico.rrrd.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.server.utils.Constants;
import pt.tecnico.rrrd.server.utils.Utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RrrdServerService extends RemoteServerGrpc.RemoteServerImplBase {
    private final Logger logger;
    private final DatabaseManager databaseManager;
    private final Map<String, String> loggedPubKeys;

    public RrrdServerService() throws IOException, ClassNotFoundException {
        this.logger = Logger.getLogger(RrrdServerApp.class.getName());
        this.databaseManager = new DatabaseManager();
        this.loggedPubKeys = new HashMap<String, String>();
    }

    private void insertLoginPubKey(String username, String key) {
        loggedPubKeys.put(username, key);
    }

    private void deleteLoginPubKey(String username) {
        loggedPubKeys.remove(username);
    }

    private String getLoggedUser() {
        return Constants.CLIENT_ID_CONTEXT_KEY.get();
    }

    public boolean addUser(String username, String password) {
        byte[] salt = getSalt();
        try {
            //Hashing and salting password
            byte[] hashedPw = getHashedAndSaltedPassword(password, salt);
            if (hashedPw == null) return false;

            databaseManager.insertUser(username, hashedPw, salt);
        } catch (SQLException e) {
            logger.info(String.format("Error creating user %s : %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
            return false;
        }
        return true;
    }

    public boolean updateUserPassword(String username, String newPassword) {
        byte[] salt = getSalt();
        try {
            byte[] hashedPw = getHashedAndSaltedPassword(newPassword, salt);
            if (hashedPw == null) return false;

            databaseManager.updateUserPassword(username, hashedPw, salt);
        } catch (SQLException e) {
            logger.info(String.format("Error updating user's %s password: %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
            return false;
        }
        return true;
    }

    public boolean deleteUser(String username) {
        try {
            databaseManager.deleteUser(username);
        } catch (SQLException e) {
            logger.info(String.format("Error deleting user %s : %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
            return false;
        }
        return true;
    }

    private byte[] getSalt() {
        //generating salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private byte[] getHashedAndSaltedPassword(String password, byte[] salt) {
        try {
            //Hashing and salting password
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] hashedPw = factory.generateSecret(spec).getEncoded();
            return hashedPw;
        } catch (InvalidKeySpecException e) {
            logger.info(String.format("KeySpec error: %s \n", e.getMessage()));
        } catch (NoSuchAlgorithmException e) {
            logger.info(String.format("Algorithm is no supported anymore: %s\n", e.getMessage()));
        }
        return null;
    }

    public boolean verifyUserPassword(String username, String password) {
        try {
            byte[] salt = databaseManager.getSalt(username);

            if (salt == null) {
                return false;
            }

            byte[] hashedPw = getHashedAndSaltedPassword(password, salt);
            return databaseManager.verifyUserPassword(username, hashedPw);
        } catch (SQLException e) {
            logger.info(String.format("SQL error: %s\n", e.getMessage()));
        }
        return false;
    }


    public boolean addPubKey(String username, String pubKey) {
        try {
            databaseManager.insertPubKey(username, pubKey);
        } catch (SQLException e) {
            logger.info(String.format("Error adding pubKey to user %s : %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
            return false;
        }
        return true;
    }

    public Map<Integer, String> getUserPubKeys(String username) {
        Map<Integer, String> pubKeys = new HashMap<>();
        try {
            pubKeys = databaseManager.getPubKeys(username);

        } catch (SQLException e) {
            logger.info(String.format("Error getting user %s pubKeys: %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
        }
        return pubKeys;
    }

    public boolean deletePubKey(String username, Integer pubKeyId) {
        try {
            databaseManager.deletePubKey(username, pubKeyId);
        } catch (SQLException e) {
            logger.info(String.format("Error deleting pubKey of user %s : %s, error code: %s\n", username, e.getMessage(), e.getErrorCode()));
            return false;
        }
        return true;
    }

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        try {
            logger.info(String.format("Received Pull Request: {Document Id: %s, Timestamp: %s}\n", request.getMessage().getDocumentId(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());
            if (verifySig && verifyTimestamp) {
                logger.info(String.format("Signature and Timestamp verified. Returning encrypted file: %s", Utils.getFileRepository(request.getMessage().getDocumentId())));
                String key = "";
                try {
                    if(!databaseManager.verifyFileExists(Utils.getFileRepository(request.getMessage().getDocumentId()))){
                        responseObserver.onError(Status.NOT_FOUND
                                .asRuntimeException());
                        return;
                    }

                    Integer pubkeyId = databaseManager.getPubKeyId(getLoggedUser(), CryptographicOperations.getStringPubKey(publicKey));
                    key = databaseManager.getPermissionKey(Utils.getFileRepository(request.getMessage().getDocumentId()), getLoggedUser(), pubkeyId);
                    if (key == null) {
                        responseObserver.onError(Status.PERMISSION_DENIED
                                .asRuntimeException());
                        return;
                    }
                } catch (SQLException e) {
                    responseObserver.onError(Status.PERMISSION_DENIED
                            .asRuntimeException()); //meaning user doesn't have access with that key
                    return;
                }


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
            publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
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

                try {
                    Integer pubkeyId = databaseManager.getPubKeyId(getLoggedUser(), CryptographicOperations.getStringPubKey(publicKey));
                    if (!databaseManager.verifyPermission(Utils.getFileRepository(pushMessage.getDocumentId()), getLoggedUser(), pubkeyId)){
                        responseObserver.onError(Status.PERMISSION_DENIED
                                .asRuntimeException());
                        return;
                    }
                } catch (SQLException e) {
                    PushResponse pushResponse = PushResponse.newBuilder().setMessage(e.getMessage()).build();
                    responseObserver.onNext(pushResponse);
                    responseObserver.onCompleted();
                }

                PrintWriter writer = new PrintWriter(Utils.getFileRepository(pushMessage.getDocumentId()), StandardCharsets.UTF_8);
                writer.println(pushMessage.getEncryptedDocument());
                writer.close();

                PushResponse pushResponse = PushResponse.newBuilder().setMessage("OK").build(); //TODO doesnt need to send OK

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
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());
            boolean fileExits = new File(Utils.getFileRepository(request.getMessage().getDocumentId())).isFile();

            if (verifySig && verifyTimestamp && !fileExits) {
                logger.info(String.format("Signature and Timestamp verified. Writing new file: %s", Utils.getFileRepository(request.getMessage().getDocumentId())));

                PrintWriter writer = new PrintWriter(Utils.getFileRepository(request.getMessage().getDocumentId()), StandardCharsets.UTF_8);
                writer.println(request.getMessage().getEncryptedDocument());
                writer.close();

                try {
                    databaseManager.insertFile(Utils.getFileRepository(request.getMessage().getDocumentId()), getLoggedUser());
                } catch (SQLException e) {
                    responseObserver.onError(Status.INTERNAL
                            .withDescription(e.getMessage())
                            .asRuntimeException());
                }

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
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        try {
            logger.info(String.format("Received deleteFile Request: {Username: %s, Timestamp: %s}\n", getLoggedUser(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");
            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }
            String filename = Utils.getFileRepository(request.getMessage().getDocumentId());

            if (!databaseManager.verifyOwner(filename, getLoggedUser())) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .asRuntimeException());
                return;
            }
            databaseManager.deleteFile(filename);
            Files.deleteIfExists(Paths.get(filename));

            responseObserver.onNext(DeleteFileResponse.newBuilder().build());
            responseObserver.onCompleted();

        } catch (IOException e) {
            responseObserver.onError(Status.DATA_LOSS //has to do with file deleteIfExists
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getPubKeys(GetPubKeysRequest request, StreamObserver<GetPubKeysResponse> responseObserver) {
        try {
            logger.info(String.format("Received GetPubKeys Request: {Username: %s, Timestamp: %s}\n", request.getMessage().getUsername(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");

                Map<Integer, String> pubKeysMap = databaseManager.getPubKeys(request.getMessage().getUsername());


                GetPubKeysResponse getPubKeysResponse = GetPubKeysResponse.newBuilder().
                        putAllPubKeys(pubKeysMap).
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
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");
                String filename = Utils.getFileRepository(request.getMessage().getDocumentId());
                try {
                    if (!databaseManager.verifyOwner(filename, getLoggedUser())) {
                        responseObserver.onError(Status.PERMISSION_DENIED
                                .asRuntimeException());
                        return;
                    }

                    Map<Integer, String> pubKeysMap = request.getMessage().getPubKeysMap();
                    for (Map.Entry<Integer, String> pubKey : pubKeysMap.entrySet()) {
                        databaseManager.insertPermission(filename, getLoggedUser(), pubKey.getKey(), pubKey.getValue());
                    }

                } catch (SQLException e) {
                    responseObserver.onError(Status.DATA_LOSS
                            .withDescription(e.getMessage())
                            .asRuntimeException());
                    return;
                }


                AddPermissionResponse addPermissionResponse = AddPermissionResponse.newBuilder().setMessage("OK").build();

                responseObserver.onNext(addPermissionResponse);
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
    public void removePermission(RemovePermissionRequest request, StreamObserver<RemovePermissionResponse> responseObserver) {
        try {
            logger.info(String.format("Received RemovePermissionRequest: {Username: %s, Timestamp: %s}\n", getLoggedUser(), request.getMessage().getTimestamp()));

            // Verify signature and ts
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");
            } else {
                String message = !verifySig ? "Invalid Signature." : "Invalid TimeStamp.";
                logger.info(message + " Aborting operation.");

                throw new InvalidParameterException(message);
            }
            String filename = Utils.getFileRepository(request.getMessage().getDocumentId());

            if (!databaseManager.verifyOwner(filename, getLoggedUser())) {
                responseObserver.onError(Status.PERMISSION_DENIED
                        .asRuntimeException());
                return;
            }

            databaseManager.deletePermission(filename, getLoggedUser());

            responseObserver.onNext(RemovePermissionResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (SQLException e) {
            responseObserver.onError(Status.DATA_LOSS //from database manager
                    .withDescription(e.getMessage())
                    .asRuntimeException());

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {

        try {
            logger.info(String.format("Received Login Request: {Username: %s, Timestamp: %s}\n", request.getMessage().getCredentials().getUsername(), request.getMessage().getTimestamp()));

            insertLoginPubKey(getLoggedUser(), request.getMessage().getClientPubKey());
            PublicKey publicKey = CryptographicOperations.convertToPublicKey(Base64.getDecoder().decode(loggedPubKeys.get(getLoggedUser())));
            boolean verifySig = CryptographicOperations.verifySignature(publicKey, request.getMessage().toByteArray(), Base64.getDecoder().decode(request.getSignature()));
            boolean verifyTimestamp = CryptographicOperations.verifyTimestamp(request.getMessage().getTimestamp());

            if (verifySig && verifyTimestamp) {
                logger.info("Signature and Timestamp verified.");

                //verify user
                if (!verifyUserPassword(
                        request.getMessage().getCredentials().getUsername(),
                        request.getMessage().getCredentials().getPassword())) {
                    responseObserver.onError(Status.UNAUTHENTICATED
                            .asRuntimeException());
                    return;
                }

                String jws = Jwts.builder().
                        setSubject(request.getMessage().getCredentials().getUsername()).
                        signWith(CryptographicOperations.getPrivateKey("password", "asymmetric_keys"), SignatureAlgorithm.RS256).
                        setIssuedAt(new Date(System.currentTimeMillis())).
                        setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000)). // 30 MINUTE EXPIRATION
                        compact();

                LoginResponse loginResponse = LoginResponse.newBuilder().
                        setToken(jws).
                        build();

                responseObserver.onNext(loginResponse);
                responseObserver.onCompleted();
                insertLoginPubKey(request.getMessage().getCredentials().getUsername(), request.getMessage().getClientPubKey());

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
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        logger.info("Received Logout request\n");

        System.out.printf(getLoggedUser());  // GET CLIENT USERNAME FORM TOKEN

        responseObserver.onNext(LogoutResponse.newBuilder().build());
        responseObserver.onCompleted();

        deleteLoginPubKey(getLoggedUser());
    }
}
