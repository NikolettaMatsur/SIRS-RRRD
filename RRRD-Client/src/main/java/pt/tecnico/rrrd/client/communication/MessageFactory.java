package pt.tecnico.rrrd.client.communication;

import pt.tecnico.rrrd.contract.PullRequest;
import pt.tecnico.rrrd.contract.PullMessage;
import pt.tecnico.rrrd.contract.PushMessage;
import pt.tecnico.rrrd.contract.PushRequest;

public class MessageFactory {


    public MessageFactory() {
//        this.jsonFormat = new JsonFormat();
    }

    public static PullRequest createPullRequestMessage(String documentId) {
        PullMessage pullMessage = PullMessage.newBuilder().setDocumentId(documentId).build();

        return PullRequest.newBuilder().setMessage(pullMessage).build();
    }

    public PushRequest createPushRequestMessage(String documentId, String document) {

        PushMessage pushMessage = PushMessage.newBuilder().setDocumentId(documentId).build();

        return PushRequest.newBuilder().setMessage(pushMessage).build();
    }


}