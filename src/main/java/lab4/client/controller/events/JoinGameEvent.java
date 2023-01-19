package lab4.client.controller.events;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lombok.Getter;

public final class JoinGameEvent extends UserEvent {
    @Getter private final NetNode node;
    @Getter private final SnakesProto.GameConfig config;

    public JoinGameEvent(NetNode node, SnakesProto.GameConfig config) {
        super(EventType.JOIN_GAME);
        this.node = node;
        this.config = config;
    }
}
