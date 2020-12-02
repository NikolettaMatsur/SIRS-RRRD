package pt.tecnico.rrrd.backup;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RrrdBackupApp {

    private final Server server;
    private final Logger logger;

    public RrrdBackupApp(String address, int port){
        this.logger = Logger.getLogger(RrrdBackupApp.class.getName());
        this.server = this.initialize(address, port);
    }

    public Server initialize(String address, int port){
        return NettyServerBuilder.forAddress(new InetSocketAddress(address, port))
                .addService(new RrrdBackupService())
                .build();
    }

    public void start() throws IOException {
        if (this.server != null) {
            logger.info("Backup Server started");
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

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s address port ", Server.class.getName());
            return;
        }

        final String address = args[0];
        final int port = Integer.parseInt(args[1]);

        RrrdBackupApp serverApp = new RrrdBackupApp(address, port);

        serverApp.start();
        serverApp.blockUntilShutdown();

    }


}
