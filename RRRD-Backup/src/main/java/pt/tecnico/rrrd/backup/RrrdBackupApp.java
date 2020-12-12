package pt.tecnico.rrrd.backup;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RrrdBackupApp {

    private final Server server;
    private final Logger logger;
    static String address;
    static int port;
    static int max_versions;
    private final String certChainFilePath = "backup.crt";
    private final String privateKeyFilePath = "backup.key";
    private final String trustCertCollectionFilePath = "ca.crt";
    private static String keyStorePassword = "password";


    public RrrdBackupApp(String address, int port) throws URISyntaxException, SSLException {
        this.logger = Logger.getLogger(RrrdBackupApp.class.getName());
        this.server = this.initialize(address, port);
    }

    public Server initialize(String address, int port) throws URISyntaxException, SSLException {
        return NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                .addService(new RrrdBackupService(this.keyStorePassword))
                .sslContext(getSslContextBuilder().build())
                .build();
    }

    public void start() throws IOException {
        if (this.server != null) {
            logger.info(String.format("Backup Server started at port %d", port));
            this.server.start();
        }
    }

    public void stop() throws InterruptedException {
        if (this.server != null) {
            this.server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
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

        sslClientContextBuilder.trustManager(new File(getClass().getClassLoader().getResource(trustCertCollectionFilePath).toURI()));
        sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
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
        max_versions = Integer.parseInt(args[2]);

        RrrdBackupApp serverApp = new RrrdBackupApp(address, port);

        serverApp.start();
        serverApp.blockUntilShutdown();

    }


}
