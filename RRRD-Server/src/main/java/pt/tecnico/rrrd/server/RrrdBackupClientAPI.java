package pt.tecnico.rrrd.server;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerStub;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerBlockingStub;


public class RrrdBackupClientAPI {

    private final BackupServerBlockingStub blockingStub;
    private final BackupServerStub asyncStub;

    public RrrdBackupClientAPI(BackupServerBlockingStub blockingStub, BackupServerStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    public UpdateResponse update() {
        System.out.println("Sending update request");

        Document document1 = Document.newBuilder().setDocumentId("1").setData(ByteString.EMPTY).setSignature("0").build();
        Document document2 = Document.newBuilder().setDocumentId("2").setData(ByteString.EMPTY).setSignature("0").build();

        UpdateRequest updateRequest = UpdateRequest.newBuilder().addDocumentList(document1).addDocumentList(document2).setSignature("0").build();

        UpdateResponse updateResponse = blockingStub.update(updateRequest);
        String status = updateResponse.getStatus();

        System.out.printf("Received update response - Status: %s\n", status);

        return updateResponse;
    }

}
