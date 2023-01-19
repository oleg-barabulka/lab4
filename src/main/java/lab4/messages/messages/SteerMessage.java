package lab4.messages.messages;

import lab4.SnakesProto;
import lombok.Getter;

public final class SteerMessage extends Message {
    @Getter private final SnakesProto.Direction direction;

    public SteerMessage(SnakesProto.Direction direction, long messageSequence, int senderID, int receiverID) {
        super(MessageType.STEER, messageSequence, senderID, receiverID);
        this.direction = direction;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        var steerBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
        steerBuilder.setDirection(direction);
        builder.setSteer(steerBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
