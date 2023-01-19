package lab4.messages.messages;

import lab4.SnakesProto;
import lombok.Getter;

public final class ErrorMessage extends Message {
    @Getter private final String errorMessage;

    public ErrorMessage(String errorMessage, long messageSequence, int senderID, int receiverID) {
        super(MessageType.ERROR, messageSequence, senderID, receiverID);
        this.errorMessage = errorMessage;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();

        var errorBuilder = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        errorBuilder.setErrorMessage(errorMessage);

        builder.setError(errorBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
