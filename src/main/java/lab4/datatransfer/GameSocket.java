package lab4.datatransfer;

import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import lab4.messages.MessageOwner;
import lab4.messages.MessageParser;
import lab4.messages.messages.AckMessage;
import lab4.messages.messages.Message;
import lab4.messages.messages.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@RequiredArgsConstructor
public final class GameSocket implements RDTSocket {
    private static final Logger logger = Logger.getLogger(GameSocket.class);

    private final DatagramSocket socket;
    private final NetworkInterface networkInterface;
    private final int sendDelayMs;

    private final Map<Long, Message> responses = new HashMap<>();
    private final Map<Instant, MessageOwner> receivedMessages = new HashMap<>();
    private final Map<Long, TimerTask> sendTasks = new HashMap<>();

    private Thread receiver;
    private Timer timer = new Timer();

    public GameSocket(NetworkInterface networkInterface, int sendDelayMs) throws IOException {
        this.socket = new DatagramSocket();
        this.sendDelayMs = sendDelayMs;
        this.networkInterface = networkInterface;
    }

    @Override
    public void start() {
        receiver = new Thread(new Receiver());
        receiver.start();
        timer = new Timer();
    }

    @Override
    public void stop() {
        if (receiver != null) {
            receiver.interrupt();
        }
        timer.cancel();
        responses.clear();
        receivedMessages.clear();
        sendTasks.clear();
    }

    @Override
    public InetAddress getAddress() {
        return networkInterface.getInetAddresses().nextElement();
    }

    @Override
    public int getPort() {
        return socket.getLocalPort();
    }

    @Override
    public Message send(Message message, NetNode receiver) {
        TimerTask task = sendMessages(message, receiver);

        Message result;
        synchronized (sendTasks) {
            sendTasks.put(message.getMessageSequence(), task);
            while (sendTasks.containsKey(message.getMessageSequence())) {
                try {
                    sendTasks.wait();
                }
                catch (InterruptedException exception) {
                    logger.error(exception.getLocalizedMessage());
                    task.cancel();
                    sendTasks.remove(message.getMessageSequence());
                    return null;
                }
            }
        }

        synchronized (responses) {
            result = responses.get(message.getMessageSequence());
            responses.remove(message.getMessageSequence());
        }
        return result;
    }

    @Override
    public void sendNonBlocking(Message message, NetNode receiver) {
        TimerTask task = sendMessages(message, receiver);
        synchronized (sendTasks) {
            sendTasks.put(message.getMessageSequence(), task);
        }
    }

    private TimerTask sendMessages(Message message, NetNode receiver) {
        DatagramPacket packet = MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort());
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.send(packet);
                }
                catch (IOException exception) {
                    logger.error(exception.getMessage());
                }
            }
        };
        timer.schedule(task, 0, sendDelayMs);
        return task;
    }

    @Override
    public void sendWithoutConfirm(Message message, NetNode receiver) {
        try {
            socket.send(MessageParser.serializeMessage(message, receiver.getAddress(), receiver.getPort()));
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public MessageOwner receive() {
        MessageOwner received;
        synchronized (receivedMessages) {
            while (receivedMessages.isEmpty()) {
                try {
                    receivedMessages.wait();
                }
                catch (InterruptedException exception) {
                    logger.error(exception.getLocalizedMessage());
                }
            }

            Instant key = receivedMessages.keySet().stream().findFirst().get();
            received = receivedMessages.get(key);
            receivedMessages.remove(key);
        }
        return received;
    }

    @Override
    public void removePendingMessage(long messageSequence) {
        synchronized (sendTasks) {
            sendTasks.remove(messageSequence);
            sendTasks.notifyAll();
        }
    }

    private void addReceivedMessage(NetNode sender, Message gameMessage) {
        if (gameMessage.getType().equals(MessageType.ACK) || gameMessage.getType().equals(MessageType.ERROR)) {
            synchronized (responses) {
                responses.put(gameMessage.getMessageSequence(), gameMessage);
            }
            synchronized (sendTasks) {
                long seq = gameMessage.getMessageSequence();
                TimerTask task = sendTasks.get(seq);
                if (task != null) {
                    task.cancel();
                }
                sendTasks.remove(seq);
                sendTasks.notifyAll();
            }
            if (gameMessage.getType().equals(MessageType.ACK)) return;
        }
        if (gameMessage.getType().isNeedConfirmation()) {
            AckMessage ack = new AckMessage(gameMessage.getMessageSequence(), gameMessage.getReceiverID(), gameMessage.getSenderID());
            sendWithoutConfirm(ack, sender);
        }
        synchronized (receivedMessages) {
            receivedMessages.put(Instant.now(), new MessageOwner(gameMessage, sender));
            receivedMessages.notifyAll();
        }
    }

    private class Receiver implements Runnable {
        private static final int PACKET_SIZE = 4096;

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                    socket.receive(packet);
                    Message message = MessageParser.deserializeMessage(packet);
                    addReceivedMessage(new NetNode(packet.getAddress(), packet.getPort()), message);
                }
                catch (IOException exception) {
                    logger.error(exception.getMessage());
                }
            }
        }
    }
}
