package pt.tecnico.rrrd.server;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rrrd.contract.BackupServerGrpc;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerStub;
import pt.tecnico.rrrd.contract.BackupServerGrpc.BackupServerBlockingStub;

import java.awt.datatransfer.Transferable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RrrdBackupClientApp {

    private static int updateInterval = 5; //in seconds

    private final BackupServerBlockingStub blockingStub;
    private final BackupServerStub asyncStub;

    public RrrdBackupClientApp(Channel channel) {
        blockingStub = BackupServerGrpc.newBlockingStub(channel);
        asyncStub = BackupServerGrpc.newStub(channel);
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s host port ", RrrdBackupClientApp.class.getName());
            return;
        }

        final String address = args[0];
        final int port = Integer.parseInt(args[1]);

        String target = address + ":" + port;

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        RrrdBackupClientApp client = new RrrdBackupClientApp(channel);
        RrrdBackupClientAPI clientAPI = new RrrdBackupClientAPI(client.blockingStub, client.asyncStub);

        //Clean channel after Ctrl-C
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down client...");
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        });

        while(true){
            Thread.sleep(updateInterval * 1000);
            clientAPI.update();
        }

    }

}
