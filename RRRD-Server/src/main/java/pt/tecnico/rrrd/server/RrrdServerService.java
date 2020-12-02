package pt.tecnico.rrrd.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.rrrd.contract.*;

public class RrrdServerService extends RemoteServerGrpc.RemoteServerImplBase {

    public RrrdServerService(){}

    @Override
    public void pull(PullRequest request, StreamObserver<PullResponse> responseObserver) {
        System.out.println("Received pull request.");

        PullResponse pullResponse = PullResponse.newBuilder().setDocument("Doc 1").setDocumentKey("Key1").build();

        responseObserver.onNext(pullResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void push(PushRequest request, StreamObserver<PushResponse> responseObserver) {

        PushResponse pushResponse = PushResponse.newBuilder().setMessage("OK").build();

        responseObserver.onNext(pushResponse);
        responseObserver.onCompleted();
    }
}
