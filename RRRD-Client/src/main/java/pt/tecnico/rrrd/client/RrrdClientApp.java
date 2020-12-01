package pt.tecnico.rrrd.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rrrd.client.command.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class RrrdClientApp {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public RrrdClientApp(Channel channel) {
        this.blockingStub = RemoteServerGrpc.newBlockingStub(channel);
        this.asyncStub = RemoteServerGrpc.newStub(channel);
    }

    public static void main(String[] args) throws Exception{

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port ", RrrdClientApp.class.getName());
            return;
        }
        System.out.println(Arrays.toString(args));
        final String address = args[0];
        final int port = Integer.parseInt(args[1]);
        final String commandInput = args[2];

        String target = address + ":" + port ;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        RrrdClientApp client = new RrrdClientApp(channel);

        ICommandHandler commandHandler = new CommandHandler(client.blockingStub, client.asyncStub);
        ICommand command = null;
        switch (commandInput) {
            case "pull":
                command = new Pull(args[3], args[4]); // TODO output path of the document, keystore passwords
                break;
            case "push":
                command = new Push(args[3], args[4]);
        }

        command.accept(commandHandler);

        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}