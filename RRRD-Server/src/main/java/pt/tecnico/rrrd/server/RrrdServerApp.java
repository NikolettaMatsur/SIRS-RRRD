package pt.tecnico.rrrd.server;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import pt.tecnico.rrrd.server.command.*;
import pt.tecnico.rrrd.server.utils.AuthorizationServerInterceptor;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class  RrrdServerApp {

    private final Server server;
    private final Logger logger;
    static String address;
    static int port;
    static int updateInterval;
    private final String certChainFilePath = "remote.crt";
    private final String privateKeyFilePath = "remote.key";

    public RrrdServerApp(String address, int port) throws IOException, URISyntaxException, ClassNotFoundException {
        this.logger = Logger.getLogger(RrrdServerApp.class.getName());
        this.server = this.initialize(address, port);
    }

    public Server initialize(String address, int port) throws IOException, URISyntaxException, ClassNotFoundException {

        return NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                .intercept(new AuthorizationServerInterceptor())
                .addService(new RrrdServerService())
                .sslContext(getSslContextBuilder().build())
                .build();
    }

    public void start() throws IOException {
        if (this.server != null) {
            logger.info(String.format("Remote Server started at port %d", port));
            this.server.start();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    RrrdServerApp.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() throws InterruptedException {
        if (this.server != null) {
            this.server.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    private SslContextBuilder getSslContextBuilder() throws URISyntaxException {
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(getClass().getClassLoader().getResource(certChainFilePath).toURI()),
                new File(getClass().getClassLoader().getResource(privateKeyFilePath).toURI()));

        return GrpcSslContexts.configure(sslClientContextBuilder);
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s address port ", Server.class.getName());
            return;
        }

        address = args[0];
        port = Integer.parseInt(args[1]);
        updateInterval = Integer.parseInt(args[2]);
        RrrdServerApp serverApp = new RrrdServerApp(address, port);
        serverApp.start();
        ICommandHandler commandHandler = new CommandHandler();

        processInput(commandHandler);

        serverApp.blockUntilShutdown();
    }

    private static String getUserPassword(ICommandHandler commandHandler, Scanner input) {
        System.out.print("Username: ");
        String username = input.nextLine();

        System.out.print("Password: ");
        String password = new String(System.console().readPassword());

        return String.format("%s %s", username, password);
    }

    private static String getAddPubKey(ICommandHandler commandHandler, Scanner input) {
        System.out.print("Username: ");
        String username = input.nextLine();

        System.out.print("PubKey: ");
        String pubKey = input.nextLine();

        return String.format("%s %s", username, pubKey);
    }

    private static String getDeletePubKey(ICommandHandler commandHandler, Scanner input) {
        Map<Integer, String> pubKeys = new HashMap<>();
        System.out.print("Username: ");
        String username = input.nextLine();

        GetUserPubKeys getUserPubKeys = new GetUserPubKeys("get_user_pub_keys " + username);
        getUserPubKeys.accept(commandHandler);

        System.out.print("Select PubKeyId: ");
        String pubKeyId = input.nextLine();

        return String.format("%s %s", username, pubKeyId);
    }

    private static void processInput(ICommandHandler commandHandler) {
        Scanner input = new Scanner(System.in);
        ICommand command = null;
        String commandInput = null;
        String commandName = null;

        loop:
        while (true) {
            System.out.print("> ");
            commandInput = input.nextLine();
            commandName = commandInput.split(" ")[0];
            switch (commandName) {
                case "add_user":
                    command = new AddUser(getUserPassword(commandHandler, input));
                    break;
                case "update_user_password":
                    command = new UpdateUserPassword(getUserPassword(commandHandler, input));
                    break;
                case "delete_user":
                    command = new DeleteUser(commandInput);
                    break;
                case "add_pub_key":
                    command = new AddPubKey(getAddPubKey(commandHandler, input));
                    break;
                case "get_user_pub_keys":
                    command = new GetUserPubKeys(commandInput);
                    break;
                case "delete_pub_key":
                    command = new DeletePubKey(getDeletePubKey(commandHandler, input));
                    break;
                case "quit":
                case "q":
                case "exit":
                    break loop;
                case "help":
                    displayUsage();
                    break;
                default:
                    System.out.println("Command not recognized.");
                    displayUsage();
                    break;
            }
            if (command != null) {
                command.accept(commandHandler);
            }
        }
        input.close();
    }

    private static void displayUsage(){
        System.out.println("Usage:\n" +
                "  * add_user\n" +
                "  * update_user_password\n" +
                "  * delete_user <username>\n" +
                "  * add_pub_key\n" +
                "  * delete_pub_key\n" +
                "  * get_user_pub_keys username\n" +
                "After the commands, interact with shell to provide further information.");
    }
}
