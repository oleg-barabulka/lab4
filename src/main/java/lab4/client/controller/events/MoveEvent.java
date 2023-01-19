package lab4.client.controller.events;

import lab4.SnakesProto;
import lombok.Getter;

public final class MoveEvent extends UserEvent {
    @Getter private final SnakesProto.Direction direction;

    public MoveEvent(SnakesProto.Direction direction) {
        super(EventType.MOVE);
        this.direction = direction;
    }
}
