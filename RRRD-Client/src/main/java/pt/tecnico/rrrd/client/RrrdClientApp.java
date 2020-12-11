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
import pt.tecnico.rrrd.crypto.CryptographicOperations;

import javax.naming.AuthenticationException;
import javax.net.ssl.SSLException;
import java.io.File;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.util.logging.Logger;

public class RrrdClientApp {

    private RemoteServerBlockingStub blockingStub;
    private RemoteServerStub asyncStub;
    private final Logger logger;

    private final String trustCertCollectionFilePath = "ca.crt";
    private final ManagedChannel channel;

    public static String keyStorePassword;

    public RrrdClientApp(String address, int port) throws SSLException, URISyntaxException {
        this.logger = Logger.getLogger(RrrdClientApp.class.getName());
        this.channel = this.initialize(address,port);

    }

    private SslContextBuilder getSslContextBuilder() throws URISyntaxException {
        return GrpcSslContexts.forClient().trustManager(new File(getClass().getClassLoader().getResource(trustCertCollectionFilePath).toURI()));
    }

    public ManagedChannel initialize(String address, int port) throws SSLException, URISyntaxException {
        ManagedChannel channel = NettyChannelBuilder.forAddress(address, port)
                .sslContext(getSslContextBuilder().build())
                .overrideAuthority("localhost")
                .build();
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

    private static void login(ICommandHandler commandHandler, Scanner input) throws AuthenticationException {
        System.out.println("Insert your credentials");

        System.out.print("Username: ");
        String username = input.nextLine();

        System.out.print("Password: ");
        String password = new String(System.console().readPassword());
        keyStorePassword = password;

        Login login = new Login(username, password);
        login.accept(commandHandler);

        if (!login.isLoggedIn()) {
            throw new AuthenticationException("\nIncorrect username or password!");
        }

        if (!correctKeyStorePassword(keyStorePassword)) {
            throw new AuthenticationException("\nIncorrect keyStore password!");
        }

        System.out.println("\nSuccessfully logged in!\n");
    }

    private static boolean correctKeyStorePassword(String keyStorePassword) {
        try {
            CryptographicOperations.getKeyStore(keyStorePassword);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void processInput(ICommandHandler commandHandler) {
        Scanner input = new Scanner(System.in);
        ICommand command = null;
        String commandInput = null;
        String commandName = null;

        try {
            login(commandHandler, input);

            loop:
            while (true) {
                System.out.print("> ");
                commandInput = input.nextLine();
                commandName = commandInput.split(" ")[0];
                switch (commandName) {
                    case "pull":
                        command = new Pull(commandInput);
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
        } catch (AuthenticationException e) {
            System.err.println(e.getMessage());
        }

        input.close();
    }
}