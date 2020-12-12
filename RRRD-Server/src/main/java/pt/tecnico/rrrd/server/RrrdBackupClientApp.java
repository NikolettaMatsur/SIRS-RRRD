package pt.tecnico.rrrd.server;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import pt.tecnico.rrrd.contract.BackupServerGrpc;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerBlockingStub;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerStub;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class RrrdBackupClientApp {

    private int updateInterval = 5; //in seconds

    private BackupServerBlockingStub blockingStub;
    private BackupServerStub asyncStub;
    private final String trustCertCollectionFilePath = "ca.crt";
    private final String certChainFilePath = "remote.crt";
    private final String privateKeyFilePath = "remote.key";
    private final ManagedChannel channel;
    private static String keyStorePassword = "password";

    public RrrdBackupClientApp(String address, int port) throws SSLException, URISyntaxException {
        this.channel = this.initialize(address, port);
        this.updateInterval = RrrdServerApp.updateInterval;
    }

    private SslContextBuilder getSslContextBuilder() throws URISyntaxException {
        return GrpcSslContexts.forClient()
                .trustManager(new File(getClass().getClassLoader().getResource(trustCertCollectionFilePath).toURI()))
                .keyManager(new File(getClass().getClassLoader().getResource(certChainFilePath).toURI()),
                        new File(getClass().getClassLoader().getResource(privateKeyFilePath).toURI()));
    }

    public ManagedChannel initialize(String address, int port) throws SSLException, URISyntaxException {
        ManagedChannel channel = NettyChannelBuilder.forAddress(address, port).sslContext(getSslContextBuilder().build()).build();
        this.blockingStub = BackupServerGrpc.newBlockingStub(channel);
        this.asyncStub = BackupServerGrpc.newStub(channel);
        return channel;
    }


    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port mode updateFrequency\nAvailable modes: terminal, auto", RrrdBackupClientApp.class.getName());
            return;
        }

        final String address = args[0];
        final int port = Integer.parseInt(args[1]);
        final String mode = args[2];
        int updateInterval = Integer.parseInt(args[3]);
        RrrdBackupClientApp client = new RrrdBackupClientApp(address, port);
        RrrdBackupClientAPI clientAPI = new RrrdBackupClientAPI(client.blockingStub, client.asyncStub, keyStorePassword);

        //Clean channel after Ctrl-C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down client...");
                    client.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });

        if (mode.equals("auto")) {
            while (true) {
                Thread.sleep(updateInterval * 1000);
                clientAPI.update();
            }
        } else if (mode.equals("terminal")) {
            clientAPI.getVersions();
            Scanner input = new Scanner(System.in);
            System.out.print("Insert version to restore: ");
            int version = Integer.parseInt(input.nextLine());
            clientAPI.restore(version);
            client.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
