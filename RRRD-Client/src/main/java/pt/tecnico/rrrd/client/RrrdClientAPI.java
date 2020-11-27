package pt.tecnico.rrrd.client;

import pt.tecnico.rrrd.contract.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;

public class RrrdClientAPI {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public RrrdClientAPI(RemoteServerBlockingStub blockingStub, RemoteServerStub asyncStub) {
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }


    public PullResponse pull() {
        PullMessage pullMessage = PullMessage.newBuilder().setDocumentId("Id1").setTimestamp("ts1").build();

        PullRequest pullRequest = PullRequest.newBuilder().setMessage(pullMessage).setSignature("sig1").build();

        PullResponse pullResponse =  blockingStub.pull(pullRequest);

        System.out.printf("Received response: Id: %s; TimeStamp: %s;\n", pullMessage.getDocumentId(), pullMessage.getTimestamp());
        return pullResponse;
    }

}

