package lab4.client.controller.events;

import lab4.datatransfer.NetNode;
import lombok.Getter;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerPlayerEvent extends UserEvent {
    @Getter private final NetNode serverNetNode;

    public ServerPlayerEvent() throws UnknownHostException {
        super(EventType.SERVER_PLAYER);
        serverNetNode = new NetNode(InetAddress.getByName("snakes.ippolitov.me"), 9192);
    }

    public ServerPlayerEvent(NetNode node) {
        super(EventType.SERVER_PLAYER);
        serverNetNode = node;
    }
}
