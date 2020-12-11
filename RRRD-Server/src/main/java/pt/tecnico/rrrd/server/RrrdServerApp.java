package pt.tecnico.rrrd.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import pt.tecnico.rrrd.contract.RemoteServerGrpc;
import pt.tecnico.rrrd.server.utils.AuthorizationServerInterceptor;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.rmi.server.ExportException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class  RrrdServerApp {

    private final Server server;
    private final Logger logger;
    static String address;
    static int port;
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

        RrrdServerApp serverApp = new RrrdServerApp(address, port);

        serverApp.start();
        serverApp.blockUntilShutdown();

    }

}
