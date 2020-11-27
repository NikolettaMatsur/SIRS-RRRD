package pt.tecnico.rrrd.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rrrd.contract.*;

public class RrrdServerService extends RemoteServerGrpc.RemoteServerImplBase {

    public RrrdServerService(){
    }

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        System.out.println("Received pull request.");

        PullResponse response = PullResponse.newBuilder().setDocument("Doc 1").setDocumentKey("Key1").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void push(PushRequest request, StreamObserver<PushResponse> responseObserver) {

        PushResponse response = PushResponse.newBuilder().setMessage("OK").build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
