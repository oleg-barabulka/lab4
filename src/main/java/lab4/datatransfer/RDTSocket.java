package lab4.datatransfer;

import lab4.messages.MessageOwner;
import lab4.messages.messages.Message;

import java.net.InetAddress;

public interface RDTSocket {
    Message send(Message message, NetNode receiver);
    void sendNonBlocking(Message message, NetNode receiver);
    void sendWithoutConfirm(Message message, NetNode receiver);
    MessageOwner receive();

    InetAddress getAddress();
    void removePendingMessage(long messageSequence);

    int getPort();

    void start();
    void stop();
}
