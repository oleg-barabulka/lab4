package lab4.messages.messages;

import lab4.SnakesProto;
import lab4.gamehandler.GameState;
import lab4.utils.StateUtils;
import lombok.Getter;

public final class StateMessage extends Message {
    @Getter private final GameState gameState;

    public StateMessage(GameState gameState, long messageSequence, int senderID, int receiverID) {
        super(MessageType.STATE, messageSequence, senderID, receiverID);
        this.gameState = gameState;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        var stateBuilder = SnakesProto.GameMessage.StateMsg.newBuilder();
        stateBuilder.setState(StateUtils.createStateForMessage(gameState));
        builder.setState(stateBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
