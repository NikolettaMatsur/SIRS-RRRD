package pt.tecnico.rrrd.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import pt.tecnico.rrrd.client.command.*;
import pt.tecnico.rrrd.contract.RemoteServerGrpc;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerStub;
import pt.tecnico.rrrd.contract.RemoteServerGrpc.RemoteServerBlockingStub;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

public class RrrdClientApp {

    private RemoteServerBlockingStub blockingStub;
    private RemoteServerStub asyncStub;

    private final String trustCertCollectionFilePath = "ca.crt";
    private final ManagedChannel channel;

    public RrrdClientApp(String address, int port) throws SSLException, URISyntaxException {
        this.channel = this.initialize(address,port);

    }

    private SslContextBuilder getSslContextBuilder() throws URISyntaxException {
        return GrpcSslContexts.forClient().trustManager(new File(getClass().getClassLoader().getResource(trustCertCollectionFilePath).toURI()));
    }

    public ManagedChannel initialize(String address, int port) throws SSLException, URISyntaxException {
        ManagedChannel channel = NettyChannelBuilder.forAddress(address, port).sslContext(getSslContextBuilder().build()).build();
        this.blockingStub = RemoteServerGrpc.newBlockingStub(channel);
        this.asyncStub = RemoteServerGrpc.newStub(channel);
        return channel;
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port ", RrrdClientApp.class.getName());
            return;
        }
        System.out.println(Arrays.toString(args));
        final String address = args[0];
        final int port = Integer.parseInt(args[1]);

        RrrdClientApp client = new RrrdClientApp(address, port);

        ICommandHandler commandHandler = new CommandHandler(client.blockingStub, client.asyncStub);

        processInput(commandHandler);

        client.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void processInput(ICommandHandler commandHandler) {
        Scanner input = new Scanner(System.in);
        ICommand command = null;
        String commandInput = null;
        String commandName = null;
        loop:
        while (true) {
            System.out.print(">");
            commandInput = input.nextLine();
            commandName = commandInput.split(" ")[0];
            switch (commandName) {
                case "pull":
                    command = new Pull(commandInput); // TODO output path of the document, keystore passwords
                    break;
                case "push":
                    command = new Push(commandInput);
                    break;
                case "add_file":
                    command = new AddFile(commandInput);
                    break;
                case "add_permission":
                    command = new AddPermission(commandInput);
                    break;
                case "quit":
                case "q":
                case "exit":
                    break loop;
                default:
                    System.out.println("Command not recognized.");
                    break;
            }
            if (command != null) {
                command.accept(commandHandler);
            }
        }
        input.close();
    }
}