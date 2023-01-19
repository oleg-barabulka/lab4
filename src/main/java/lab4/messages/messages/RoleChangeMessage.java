package lab4.messages.messages;

import lab4.SnakesProto;
import lombok.Getter;

public final class RoleChangeMessage extends Message {
    @Getter private final SnakesProto.NodeRole senderRole;
    @Getter private final SnakesProto.NodeRole receiverRole;

    public RoleChangeMessage(SnakesProto.NodeRole senderRole, SnakesProto.NodeRole receiverRole, long messageSequence, int senderID, int receiverID) {
        super(MessageType.ROLE_CHANGE, messageSequence, senderID, receiverID);
        this.senderRole = senderRole;
        this.receiverRole = receiverRole;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        var roleBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
        roleBuilder.setSenderRole(senderRole);
        roleBuilder.setReceiverRole(receiverRole);
        builder.setRoleChange(roleBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        builder.setSenderId(getSenderID());
        builder.setReceiverId(getReceiverID());
        return builder.build();
    }
}
