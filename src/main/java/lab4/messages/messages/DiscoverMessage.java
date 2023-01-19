package lab4.messages.messages;

import lab4.SnakesProto;

public class DiscoverMessage extends Message {
    public DiscoverMessage(long messageSequence, int senderID, int receiverID) {
        super(MessageType.DISCOVER, messageSequence, senderID, receiverID);
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        SnakesProto.GameMessage.Builder build = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.DiscoverMsg.Builder discoverBuilder = SnakesProto.GameMessage.DiscoverMsg.newBuilder();
        build.setDiscover(discoverBuilder.build());
        return build.build();
    }
}
