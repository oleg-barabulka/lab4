package lab4.client.model;

import lab4.SnakesProto;
import lab4.datatransfer.GameSocket;
import lab4.datatransfer.NetNode;
import lab4.datatransfer.RDTSocket;
import lab4.gamehandler.GameState;
import lab4.gamehandler.Player;
import lab4.messages.MessageHandler;
import lab4.messages.MessageOwner;
import lab4.messages.messages.*;
import lab4.master.MasterGame;
import lab4.master.MasterHandler;
import lab4.utils.StateUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import lab4.client.view.View;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public final class ClientGame implements ClientGameHandler {
    private static final Logger logger = Logger.getLogger(ClientGame.class);
    private static final int EMPTY = -1;

    private final RDTSocket gameSocket;
    private final SnakesProto.GameConfig config;
    private final String playerName;
    @Getter private final String gameName;
    private final View view;
    private final InetSocketAddress multicastInfo;
    private final NetworkInterface networkInterface;

    private MasterHandler activeMasterGame;
    private GameState gameState;
    private SnakesProto.NodeRole nodeRole;
    private NetNode master;
    private NetNode deputy;
    private Instant masterLastSeen;

    private int masterID = EMPTY;
    private int playerID = EMPTY;

    private final AtomicLong msgSeq = new AtomicLong(0);
    private final MessageHandler messageHandler = createMessageHandler();
    private Timer timer = new Timer();

    private long curSteerMsgSeq = EMPTY;
    private SnakesProto.Direction curDirection = null;
    private Thread receiver = null;

    public ClientGame(SnakesProto.GameConfig config, String playerName, View view, InetSocketAddress multicastInfo, NetworkInterface networkInterface) throws IOException {
        int pingDelay = config.getStateDelayMs() / 10;
        this.gameSocket = new GameSocket(networkInterface, pingDelay);
        this.config = config;
        this.view = view;
        this.multicastInfo = multicastInfo;
        this.networkInterface = networkInterface;
        this.playerName = playerName;
        this.gameName = playerName + "Game";
    }

    @Override
    public void startNewGame() {
        stopCurrentMasterGame();
        gameSocket.stop();
        gameSocket.start();
        try {
            activeMasterGame = new MasterGame(config, multicastInfo, gameSocket.getAddress(), gameSocket.getPort(), playerName, networkInterface, gameName);
            master = new NetNode(gameSocket.getAddress(), activeMasterGame.getPort());
            masterLastSeen = Instant.now();
            gameState = null;
            changeNodeRole(SnakesProto.NodeRole.MASTER);

            startTimerTasks();
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
            stopCurrentMasterGame();
        }
    }

    @Override
    public void joinGame(NetNode gameOwner, String playerName) {
        gameSocket.stop();
        gameSocket.start();
        Message response = gameSocket.send(new JoinMessage(SnakesProto.PlayerType.HUMAN, playerName, gameName, SnakesProto.NodeRole.NORMAL, msgSeq.get()), gameOwner);
        if (response == null) return;

        if (response.getType().equals(MessageType.ERROR)) {
            messageHandler.handle(null, (ErrorMessage) response);
            return;
        }
        if (!response.getType().equals(MessageType.ACK)) {
            logger.error("For Join message, Server didn't respond with Ack message");
            return;
        }

        masterID = response.getSenderID();
        playerID = response.getReceiverID();
        master = gameOwner;
        masterLastSeen = Instant.now();
        deputy = null;
        gameState = null;
        changeNodeRole(SnakesProto.NodeRole.NORMAL);

        startTimerTasks();
    }

    @Override
    public void joinServerPlayer(NetNode serverNetNode) {
        AnnouncementMessage announcementMessage = new AnnouncementMessage(msgSeq.getAndIncrement());
        announcementMessage.addGame(gameState.getActivePlayers(), config, gameName, true);
        gameSocket.sendWithoutConfirm(announcementMessage, serverNetNode);
    }

    @Override
    public void handleMove(SnakesProto.Direction direction) {
        if (!nodeRole.equals(SnakesProto.NodeRole.VIEWER)) {
            gameSocket.removePendingMessage(curSteerMsgSeq);
            curSteerMsgSeq = msgSeq.getAndIncrement();
            curDirection = direction;
            gameSocket.sendNonBlocking(new SteerMessage(direction, curSteerMsgSeq, playerID, masterID), master);
        }
    }

    @Override
    public void exit() {
        if (master != null) {
            gameSocket.sendWithoutConfirm(
                    new RoleChangeMessage(SnakesProto.NodeRole.VIEWER, SnakesProto.NodeRole.MASTER, msgSeq.getAndIncrement(), playerID, masterID),
                    master
            );
        }
        nodeRole = SnakesProto.NodeRole.VIEWER;
        gameSocket.stop();
        timer.cancel();
        if (receiver != null) {
            receiver.interrupt();
            receiver = null;
        }
    }

    private void stopCurrentMasterGame() {
        if (activeMasterGame != null) {
            activeMasterGame.stop();
        }
        activeMasterGame = null;
        masterID = EMPTY;
        playerID = EMPTY;
        master = null;
        deputy = null;
        gameState = null;
    }

    private void startTimerTasks() {
        timer.cancel();
        timer = new Timer();
        startSendPingMessages();
        startHandleReceivedMessages();
        startMasterCheck();
    }

    private void startHandleReceivedMessages() {
        receiver = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                MessageOwner recv = gameSocket.receive();
                handleMessage(recv.getOwner(), recv.getMessage());
            }
        });
        receiver.start();
    }

    private void startSendPingMessages() {
        int pingDelay = config.getStateDelayMs() / 10;
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (master != null) {
                            gameSocket.sendWithoutConfirm(new PingMessage(msgSeq.getAndIncrement(), playerID, masterID), master);
                        }
                    }
                },
                0,
                pingDelay
        );
    }

    private void startMasterCheck() {
        int period = (int) (config.getStateDelayMs() * 0.8);
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (master != null && Duration.between(masterLastSeen, Instant.now()).abs().toMillis() > config.getStateDelayMs()) {
                            if (nodeRole == SnakesProto.NodeRole.DEPUTY) {
                                becomeMaster();
                            }
                            else if (nodeRole == SnakesProto.NodeRole.NORMAL) {
                                master = deputy;
                                deputy = null;
                                masterLastSeen = Instant.now();
                            }
                            if (curDirection != null) {
                                handleMove(curDirection);
                            }
                        }
                    }
                },
                0, period
        );
    }

    private void handleMessage(NetNode sender, Message message) {
        switch (message.getType()) {
            case ROLE_CHANGE -> messageHandler.handle(sender, (RoleChangeMessage) message);
            case ERROR -> messageHandler.handle(sender, (ErrorMessage) message);
            case STATE -> messageHandler.handle(sender, (StateMessage) message);
            default -> logger.error("Can't handle this message type = " + message.getType());
        }
    }

    private void becomeMaster()  {
        try {
            activeMasterGame = new MasterGame(gameState, multicastInfo, networkInterface, gameName);
            master = new NetNode(gameSocket.getAddress(), activeMasterGame.getPort());
            masterID = playerID;
            deputy = null;
            changeNodeRole(SnakesProto.NodeRole.MASTER);
            gameState.getActivePlayers().forEach(player -> {
                        if (player.getRole().equals(SnakesProto.NodeRole.NORMAL)) {
                            gameSocket.sendNonBlocking(
                                    new RoleChangeMessage(
                                            SnakesProto.NodeRole.DEPUTY,
                                            SnakesProto.NodeRole.NORMAL,
                                            msgSeq.getAndIncrement(),
                                            playerID,
                                            player.getId()
                                    ),
                                    player.getNetNode()
                            );
                        }
                    }
            );
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    private void changeNodeRole(SnakesProto.NodeRole nodeRole) {
        this.nodeRole = nodeRole;
        logger.info("Client: I am " + this.nodeRole);
    }

    private void lose() {
        System.out.println("Lose");
    }

    private MessageHandler createMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handle(NetNode sender, DiscoverMessage message) {
                logger.error("Client shouldn't receive Discover messages");
            }

            @Override
            public void handle(NetNode sender, SteerMessage message) {
                logger.error("Client shouldn't receive Steer messages");
            }

            @Override
            public void handle(NetNode sender, JoinMessage message) {
                logger.error("Client shouldn't receive Join messages");
            }

            @Override
            public void handle(NetNode sender, PingMessage message) {
                logger.error("Client shouldn't receive Ping messages");
            }

            @Override
            public void handle(NetNode sender, StateMessage stateMsg) {
                GameState newState = stateMsg.getGameState();
                if (gameState != null && gameState.getStateID() >= newState.getStateID()) {
                    logger.warn("Received state with id=" + newState.getStateID() + " less then last gamehandler state id=" + gameState.getStateID());
                    return;
                }

                master = sender;
                masterLastSeen = Instant.now();
                Player deputyPlayer = StateUtils.getDeputyFromState(newState);
                if (deputyPlayer == null) {
                    deputy = null;
                }
                else {
                    deputy = deputyPlayer.getNetNode();
                }
                gameState = newState;
                view.updateCurrentGame(newState);
            }

            @Override
            public void handle(NetNode sender, ErrorMessage message) {
                logger.error("ERROR=" + message.getErrorMessage());
            }

            @Override
            public void handle(NetNode sender, RoleChangeMessage roleChangeMsg) {
                switch (nodeRole) {
                    case DEPUTY -> {
                        if (roleChangeMsg.getSenderRole().equals(SnakesProto.NodeRole.MASTER) && roleChangeMsg.getReceiverRole().equals(SnakesProto.NodeRole.MASTER)) {
                            becomeMaster();
                            deputy = null;
                        }
                        else if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.MASTER &&
                                 roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.VIEWER) {
                            lose();
                        }
                    }
                    case NORMAL -> {
                        if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.MASTER && roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                            changeNodeRole(SnakesProto.NodeRole.DEPUTY);
                            masterLastSeen = Instant.now();
                        }
                        else if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.DEPUTY &&
                                 roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.NORMAL) {
                            master = deputy;
                            masterLastSeen = Instant.now();
                            deputy = null;
                        }
                        else if (roleChangeMsg.getSenderRole() == SnakesProto.NodeRole.MASTER &&
                                 roleChangeMsg.getReceiverRole() == SnakesProto.NodeRole.VIEWER) {
                            lose();
                        }
                    }
                    default -> logger.error("Received role change message: me=" + nodeRole +
                                            ", senderRole=" + roleChangeMsg.getSenderRole() +
                                            ", receiverRole=" + roleChangeMsg.getReceiverRole());
                }
            }
        };
    }
}
