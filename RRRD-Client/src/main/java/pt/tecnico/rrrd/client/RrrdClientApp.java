package pt.tecnico.rrrd.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rrrd.contract.RemoteServerGrpc;
import pt.tecnico.rrrd.crypto.CryptographicOperations;
import pt.tecnico.rrrd.client.communication.MessageFactory;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;

import java.security.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class RrrdClientApp {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public RrrdClientApp(Channel channel) {
        blockingStub = RemoteServerGrpc.newBlockingStub(channel);
        asyncStub = RemoteServerGrpc.newStub(channel);
    }

    public static void main(String[] args) throws Exception{

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port ", RrrdClientApp.class.getName());
            return;
        }

        final String address = args[0];
        final int port = Integer.parseInt(args[1]);

        String target = address + ":" + port ;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        RrrdClientApp client = new RrrdClientApp(channel);
        RrrdClientAPI clientAPI = new RrrdClientAPI(client.blockingStub, client.asyncStub);

        clientAPI.pull();

        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }
}