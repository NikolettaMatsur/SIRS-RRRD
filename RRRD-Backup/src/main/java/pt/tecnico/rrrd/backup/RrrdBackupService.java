package pt.tecnico.rrrd.backup;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rrrd.contract.*;

public class RrrdBackupService extends BackupServerGrpc.BackupServerImplBase {
    public RrrdBackupService(){}


    @Override
    public void update(UpdateRequest request, StreamObserver<UpdateResponse> responseObserver) {
        System.out.println("Received update request.");

        UpdateResponse updateResponse = UpdateResponse.newBuilder().setStatus("NOT_IMPLEMENTED").setSignature("0").build();

        responseObserver.onNext(updateResponse);
        responseObserver.onCompleted();

    }

    @Override
    public void restore(RestoreRequest request, StreamObserver<RestoreResponse> responseObserver) {

    }
}

