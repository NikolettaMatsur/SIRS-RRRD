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
import java.util.Scanner;

public class RrrdClientApp {

    private final RemoteServerBlockingStub blockingStub;
    private final RemoteServerStub asyncStub;

    public RrrdClientApp(Channel channel) {
        this.blockingStub = RemoteServerGrpc.newBlockingStub(channel);
        this.asyncStub = RemoteServerGrpc.newStub(channel);
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
        String target = address + ":" + port;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        RrrdClientApp client = new RrrdClientApp(channel);
        ICommandHandler commandHandler = new CommandHandler(client.blockingStub, client.asyncStub);


        processInput(commandHandler);


        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
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