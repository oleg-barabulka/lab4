package lab4.multicastreceiver;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lab4.gamehandler.Player;
import org.apache.log4j.Logger;
import lab4.client.view.View;
import lab4.messages.MessageParser;
import lab4.messages.messages.Message;
import lab4.messages.messages.MessageType;
import lab4.messages.messages.AnnouncementMessage;

import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MulticastReceiver {
    private static final Logger logger = Logger.getLogger(MulticastReceiver.class);
    private static final int BUFFER_SIZE = 4096;
    private static final int SO_TIMEOUT_MS = 3000;

    private final View view;
    private final InetSocketAddress multicastInfo;
    private final NetworkInterface networkInterface;
    private final Thread checkerThread;

    private final Map<GameInfo, Instant> gameInfos = new HashMap<>();

    public MulticastReceiver(InetSocketAddress multicastInfo, View view, NetworkInterface networkInterface) {
        validateAddress(multicastInfo.getAddress());
        this.multicastInfo = multicastInfo;
        this.networkInterface = networkInterface;
        this.view = view;
        this.checkerThread = new Thread(receiveMulticast());
    }

    private void validateAddress(InetAddress multicastAddress) {
        if (!multicastAddress.isMulticastAddress()) {
            throw new IllegalArgumentException(multicastAddress + " is not multicast address");
        }
    }

    public void start() {
        checkerThread.start();
    }

    public void stop() {
        checkerThread.interrupt();
    }

    private Runnable receiveMulticast() {
        return () -> {
            try (MulticastSocket socket = new MulticastSocket(multicastInfo.getPort())) {
                byte[] buffer = new byte[BUFFER_SIZE];

                socket.joinGroup(multicastInfo, networkInterface);
                socket.setSoTimeout(SO_TIMEOUT_MS);

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);
                    try {
                        socket.receive(datagramPacket);
                        NetNode sender = new NetNode(datagramPacket.getAddress(), datagramPacket.getPort());
                        Message message = MessageParser.deserializeMessage(datagramPacket);
                        if (message.getType().equals(MessageType.ANNOUNCEMENT)) {
                            List<SnakesProto.GameAnnouncement> games = ((AnnouncementMessage) message).getGames();
                            games.forEach(s -> gameInfos.put(createGameInfo(sender, s), Instant.now()));
                        }
                    }
                    catch (SocketTimeoutException ignored) {
                    }
                    gameInfos.entrySet().removeIf(entry ->
                            Duration.between(entry.getValue(), Instant.now()).abs().toMillis() >= SO_TIMEOUT_MS);
                    view.updateGameList(gameInfos.keySet());
                }
                socket.leaveGroup(multicastInfo, networkInterface);
            } catch (IOException exception) {
                logger.error("Problem with multicast socket on port=" + multicastInfo.getPort(), exception);
            }
        };
    }

    private GameInfo createGameInfo(NetNode sender, SnakesProto.GameAnnouncement game){
        List<Player> players = game.getPlayers().getPlayersList()
                                   .stream()
                                   .map(s -> {
                                       try {
                                           return new Player(s.getName(), s.getId(), new NetNode(s.getIpAddress(), s.getPort()), s.getRole(), s.getScore());
                                       } catch (UnknownHostException e) {
                                           throw new IllegalStateException("UnknownHost " + e);
                                       }
                                   })
                                   .toList();
        return new GameInfo(game.getGameName(), game.getConfig(), sender, players, game.getCanJoin());
    }
}