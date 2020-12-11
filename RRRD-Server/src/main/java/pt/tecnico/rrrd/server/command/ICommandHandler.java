package pt.tecnico.rrrd.server.command;

import java.util.Map;

public interface ICommandHandler {

    void handle(AddUser addUser);

    void handle(UpdateUserPassword updateUserPassword);

    void handle(DeleteUser deleteUser);

    void handle(AddPubKey addPubKey);

    void handle(DeletePubKey deletePubKey);

    void handle(GetUserPubKeys getUserPubKeys);

}
