package lab4.master;

import lab4.SnakesProto;
import lab4.datatransfer.GameSocket;
import lab4.datatransfer.NetNode;
import lab4.datatransfer.RDTSocket;
import lab4.gamehandler.game.GameHandler;
import lab4.gamehandler.GameState;
import lab4.gamehandler.Player;
import lab4.gamehandler.game.Game;
import lab4.messages.MessageHandler;
import lab4.messages.MessageOwner;
import lab4.messages.messages.*;
import lab4.utils.PlayerUtils;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class MasterGame implements MasterHandler {
    private static final int EMPTY = -1;

    private static final Logger logger = Logger.getLogger(MasterGame.class);
    public static final int ANNOUNCEMENT_SEND_PERIOD_MS = 1000;
    private final GameHandler game;
    private final SnakesProto.GameConfig gameConfig;
    private final Map<Player, SnakesProto.Direction> playersMoves = new ConcurrentHashMap<>();
    private final Map<Player, Instant> playersLastSeen = new ConcurrentHashMap<>();
    private final AtomicLong msgSeq = new AtomicLong(0);
    private final MessageHandler messageHandler = createMessageHandler();
    private final String gameName;

    @Getter private final RDTSocket socket;
    private final InetSocketAddress multicastAddress;

    private Timer timer = new Timer();
    private Thread receiveThread = null;
    private Player masterPlayer = null;
    private Player deputyPlayer = null;

    public MasterGame(SnakesProto.GameConfig gameConfig, InetSocketAddress multicastAddress,
                      InetAddress masterAddress, int masterPort, String masterName,
                      NetworkInterface networkInterface, String gameName) throws IOException {
        this.gameConfig = gameConfig;
        this.game = new Game(gameConfig, this);
        this.multicastAddress = multicastAddress;
        this.socket = new GameSocket(networkInterface, gameConfig.getStateDelayMs() / 10);
        this.socket.start();
        this.gameName = gameName;

        Optional<Player> playerOptional = registerNewPlayer(new NetNode(masterAddress, masterPort), masterName);
        playerOptional.ifPresent(player -> {
            player.setRole(SnakesProto.NodeRole.MASTER);
            player.setScore(0);
            masterPlayer = player;
        });

        startTimerTasks();
        startReceivingMessages();
    }

    public MasterGame(GameState gameState, InetSocketAddress multicastAddress, NetworkInterface networkInterface, String gameName) throws IOException {
        gameConfig = gameState.getGameConfig();
        game = new Game(gameState, this);
        this.multicastAddress = multicastAddress;
        this.gameName = gameName;
        socket = new GameSocket(networkInterface, gameConfig.getStateDelayMs() / 10);
        socket.start();

        gameState.getActivePlayers().forEach(player -> {
            player.setRole(player.getRole().equals(SnakesProto.NodeRole.MASTER) ? SnakesProto.NodeRole.VIEWER : player.getRole());
            player.setRole(player.getRole().equals(SnakesProto.NodeRole.DEPUTY) ? SnakesProto.NodeRole.MASTER : player.getRole());
            if (player.getRole().equals(SnakesProto.NodeRole.MASTER)) {
                masterPlayer = player;
            }
            chooseNewDeputy();
            playersLastSeen.put(player, Instant.now());
            if (!player.getRole().equals(SnakesProto.NodeRole.VIEWER)) {
                playersMoves.put(player, game.getSnakeByPlayer(player).getDirection());
            }
        });

        startTimerTasks();
        startReceivingMessages();
    }

    @Override
    public void update(GameState gameState) {
        playersLastSeen.keySet()
                .forEach(player -> socket.sendWithoutConfirm(
                                new StateMessage(gameState, msgSeq.getAndIncrement(), masterPlayer.getId(), player.getId()),
                                player.getNetNode()
                            )
                );
        if (gameState.getActivePlayers().isEmpty()) {
            stop();
        }
    }

    @Override
    public int getPort() {
        return socket.getPort();
    }

    @Override
    public void stop() {
        if (deputyPlayer != null) {
            socket.sendWithoutConfirm(
                    new RoleChangeMessage(SnakesProto.NodeRole.MASTER, SnakesProto.NodeRole.MASTER, msgSeq.getAndIncrement(), masterPlayer.getId(), deputyPlayer.getId()),
                    deputyPlayer.getNetNode()
            );
        }
        timer.cancel();
        receiveThread.interrupt();
        socket.stop();
    }

    private void startTimerTasks() {
        timer.cancel();
        timer = new Timer();
        startGameUpdateTimer();
        startSendAnnouncementMessages();
        startRemovingDisconnectedPlayers();
    }

    private void startGameUpdateTimer() {
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        game.moveAllSnakes(Map.copyOf(playersMoves));
                        playersMoves.clear();
                    }
                },
                0, gameConfig.getStateDelayMs());
    }

    private void startSendAnnouncementMessages() {
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        AnnouncementMessage announcementMessage = new AnnouncementMessage(msgSeq.getAndIncrement());
                        announcementMessage.addGame(playersLastSeen.keySet().stream().toList(), gameConfig, gameName, true);
                        socket.sendWithoutConfirm(
                                announcementMessage,
                                new NetNode(multicastAddress.getAddress(), multicastAddress.getPort())
                        );
                    }
                },
                0, ANNOUNCEMENT_SEND_PERIOD_MS);
    }

    private void startRemovingDisconnectedPlayers() {
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        playersLastSeen.forEach((player, lastSeen) -> {
                            if (isDisconnected(lastSeen)) {
                                game.removePlayer(player);
                                playersMoves.remove(player);
                            }
                        });
                        playersLastSeen.entrySet().removeIf(entry -> isDisconnected(entry.getValue()));

                        if (deputyPlayer != null && !playersLastSeen.containsKey(deputyPlayer)) {
                            deputyPlayer = null;
                            chooseNewDeputy();
                        }
                    }
                },
                0,gameConfig.getStateDelayMs() / 10);
    }

    private void startReceivingMessages() {
        receiveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                MessageOwner receive = socket.receive();
                handleMessage(receive.getOwner(), receive.getMessage());
            }
        });
        receiveThread.start();
    }

    private void registerNewMove(Player player, SnakesProto.Direction direction) {
        playersMoves.put(player, direction);
    }

    private void handleMessage(NetNode sender, Message message) {
        switch (message.getType()) {
            case DISCOVER -> messageHandler.handle(sender, (DiscoverMessage) message);
            case ROLE_CHANGE -> messageHandler.handle(sender, (RoleChangeMessage) message);
            case STEER -> messageHandler.handle(sender, (SteerMessage) message);
            case JOIN -> messageHandler.handle(sender, (JoinMessage) message);
            case PING -> messageHandler.handle(sender, (PingMessage) message);
            case ERROR -> messageHandler.handle(sender, (ErrorMessage) message);
            default -> throw new IllegalStateException("Server: Cant handle this message type = " + message.getType());
        }
    }

    private void chooseNewDeputy() {
        Optional<Player> playerOptional = playersLastSeen.keySet().stream().filter(player -> player.getRole() != SnakesProto.NodeRole.MASTER).findAny();
        playerOptional.ifPresentOrElse(
                this::setDeputyPlayer,
                () -> logger.warn("Can't chose deputy")
        );
    }

    private void setDeputyPlayer(Player deputy) {
        Message response = socket.send(
                new RoleChangeMessage(SnakesProto.NodeRole.MASTER, SnakesProto.NodeRole.DEPUTY, msgSeq.getAndIncrement(), masterPlayer.getId(), deputy.getId()),
                deputy.getNetNode()
        );
        if (response instanceof AckMessage) {
            deputyPlayer = deputy;
            deputyPlayer.setRole(SnakesProto.NodeRole.DEPUTY);
        }
    }

    private boolean isDisconnected(Instant moment) {
        return Duration.between(moment, Instant.now()).abs().toMillis() >= gameConfig.getStateDelayMs() * 0.8;
    }

    private boolean validateNewPlayer(NetNode sender, JoinMessage joinMsg) {
        if (playersLastSeen.keySet().stream().anyMatch(player -> player.getName().equals(joinMsg.getPlayerName()))) {
            logger.error("Node=" + sender + ": " + "Player with name '" + joinMsg.getPlayerName() + "' already registered as player");
            socket.sendWithoutConfirm(
                    new ErrorMessage("Player with name already exist", joinMsg.getMessageSequence(), masterPlayer.getId(), EMPTY),
                    sender
            );
            return false;
        }
        return true;
    }

    private Optional<Player> registerNewPlayer(NetNode netNode, String playerName) {
        try {
            Player player = game.registerNewPlayer(playerName, netNode);
            playersLastSeen.put(player, Instant.now());
            playersMoves.put(player, game.getSnakeByPlayer(player).getDirection());
            return Optional.of(player);
        }
        catch (IllegalStateException exception) {
            String errorMessage = "Can't place player on field because no space";
            logger.debug(errorMessage);
            this.socket.sendWithoutConfirm(
                    new ErrorMessage(errorMessage, msgSeq.getAndIncrement(), masterPlayer.getId(), EMPTY),
                    netNode
            );
            return Optional.empty();
        }
    }

    private void removePlayer(NetNode sender) {
        Player player = PlayerUtils.findPlayerByAddress(sender, playersLastSeen.keySet());
        if (player == null) {
            return;
        }

        updateLastSeen(sender);
        playersMoves.remove(player);
        game.removePlayer(player);
    }

    private void updateLastSeen(NetNode sender) {
        Player player = PlayerUtils.findPlayerByAddress(sender, playersLastSeen.keySet());
        if (player != null) {
            playersLastSeen.put(player, Instant.now());
        }
    }

    private MessageHandler createMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handle(NetNode sender, SteerMessage message) {
                updateLastSeen(sender);
                Player player = PlayerUtils.findPlayerByAddress(sender, playersLastSeen.keySet());
                if (player == null) return;

                registerNewMove(player, message.getDirection());
                logger.debug("NetNode=" + sender + " as player=" + player + " make move with direction=" + message.getDirection());
            }

            @Override
            public void handle(NetNode sender, JoinMessage message) {
                if (!validateNewPlayer(sender, message)) return;
                registerNewPlayer(sender, message.getPlayerName())
                        .ifPresent(player -> {
                                    logger.debug("NetNode=" + sender + " was successfully registered as player=" + player);
                                    player.setRole(SnakesProto.NodeRole.NORMAL);
                                    player.setScore(0);
                                    if (deputyPlayer == null) {
                                        chooseNewDeputy();
                                    }
                                }
                        );
            }

            @Override
            public void handle(NetNode sender, PingMessage message) {
                updateLastSeen(sender);
            }

            @Override
            public void handle(NetNode sender, StateMessage message) {
                throw new IllegalStateException("Server shouldn't receive State messages");
            }

            @Override
            public void handle(NetNode sender, ErrorMessage message) {
                logger.error(message.getErrorMessage());
            }

            @Override
            public void handle(NetNode sender, RoleChangeMessage message) {
                if (message.getReceiverRole().equals(SnakesProto.NodeRole.MASTER) && message.getSenderRole().equals(SnakesProto.NodeRole.VIEWER)) {
                    removePlayer(sender);
                }
                else {
                    logger.warn("Server: Unsupported roles at role change message=" + message + " from=" + sender);
                    throw new IllegalArgumentException("Unsupported roles at role change message=" + message + " from=" + sender);
                }
            }

            @Override
            public void handle(NetNode sender, DiscoverMessage message) {
                logger.info("Discover message from " + sender.getAddress() + " " + sender.getPort());
                AnnouncementMessage announcementMessage = new AnnouncementMessage(msgSeq.getAndIncrement());
                announcementMessage.addGame(playersLastSeen.keySet().stream().toList(), gameConfig, gameName, true);
                socket.sendWithoutConfirm(announcementMessage, sender);
            }
        };
    }
}