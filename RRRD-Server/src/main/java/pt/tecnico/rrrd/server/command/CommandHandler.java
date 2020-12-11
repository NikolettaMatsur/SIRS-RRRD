package pt.tecnico.rrrd.server.command;

import pt.tecnico.rrrd.server.RrrdServerService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CommandHandler implements ICommandHandler{
    private final Logger logger;
    private RrrdServerService serverService;

    public CommandHandler() throws IOException, ClassNotFoundException {
        this.logger = Logger.getLogger(CommandHandler.class.getName());
        this.serverService = new RrrdServerService();
    }

    @Override
    public void handle(AddUser addUser) {
        if (serverService.addUser(addUser.getUsername(), addUser.getPassword())){
            logger.info(String.format("User %s created successfully.", addUser.getUsername()));
        } else {
            logger.info(String.format("Error in creating %s user.", addUser.getUsername()));
        }
    }

    @Override
    public void handle(UpdateUserPassword updateUserPassword) {
        if (serverService.updateUserPassword(updateUserPassword.getUsername(), updateUserPassword.getNewPassword())){
            logger.info(String.format("User %s password updated successfully.", updateUserPassword.getUsername()));
        } else {
            logger.info(String.format("Error in updating password %s user.", updateUserPassword.getUsername()));
        }
    }

    @Override
    public void handle(DeleteUser deleteUser) {
        if (serverService.deleteUser(deleteUser.getUsername())){
            logger.info(String.format("Deleted user %s successfully.", deleteUser.getUsername()));
        } else {
            logger.info(String.format("Error in deleting user %s.", deleteUser.getUsername()));
        }
    }

    @Override
    public void handle(AddPubKey addPubKey) {
        if (serverService.addPubKey(addPubKey.getUsername(), addPubKey.getPubKey())){
            logger.info(String.format("PubKey added to user %s successfully.", addPubKey.getUsername()));
        } else {
            logger.info(String.format("Error in adding pubkey to %s user.", addPubKey.getUsername()));
        }
    }

    @Override
    public void handle(DeletePubKey deletePubKey) {
        if (serverService.deletePubKey(deletePubKey.getUsername(), deletePubKey.getPubKeyId())){
            logger.info(String.format("PubKey deleted from user %s successfully.", deletePubKey.getUsername()));
        } else {
            logger.info(String.format("Error in deleting pubkey from %s user.", deletePubKey.getUsername()));
        }
    }

    @Override
    public void handle(GetUserPubKeys getUserPubKeys) {
        Map<Integer, String> pubKeys = new HashMap<>();
        pubKeys = serverService.getUserPubKeys(getUserPubKeys.getUsername());

        for (Map.Entry<Integer, String> entry : pubKeys.entrySet()) {
            System.out.println(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
        }
        if (pubKeys.entrySet().size() <= 0) {
            System.out.println("O utilizador não tem chaves públicas associadas.");
        }
    }
}
