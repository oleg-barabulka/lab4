package lab4.messages.messages;

import lab4.SnakesProto;

public final class AckMessage extends Message {
    public AckMessage(long messageSequence, int senderID, int receiverID) {
        super(MessageType.ACK, messageSequence, senderID, receiverID);
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        SnakesProto.GameMessage.Builder builder = SnakesProto.GameMessage.newBuilder();
        builder.setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build());
        builder.setMsgSeq(getMessageSequence());
        builder.setSenderId(getSenderID());
        builder.setReceiverId(getReceiverID());
        return builder.build();
    }
}
