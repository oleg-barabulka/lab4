package lab4.messages.messages;

import lab4.SnakesProto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


import java.io.Serializable;

@RequiredArgsConstructor
public abstract class Message implements Serializable {
    @Getter private final MessageType type;
    @Getter private final long messageSequence;
    @Getter private final int senderID;
    @Getter private final int receiverID;

    public abstract SnakesProto.GameMessage getGameMessage();
}
