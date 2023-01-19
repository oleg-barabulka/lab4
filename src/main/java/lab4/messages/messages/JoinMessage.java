package lab4.messages.messages;

import lab4.SnakesProto;
import lombok.Getter;

public final class JoinMessage extends Message {
    private static final int EMPTY = -1;
    @Getter private final SnakesProto.PlayerType playerType;
    @Getter private final String playerName;
    @Getter private final SnakesProto.NodeRole role;
    @Getter private final String gameName;

    public JoinMessage(SnakesProto.PlayerType playerType, String playerName, String gameName, SnakesProto.NodeRole role, long messageSequence) {
        super(MessageType.JOIN, messageSequence, EMPTY, EMPTY);
        this.playerType = playerType;
        this.playerName = playerName;
        this.gameName = gameName;
        this.role = role;
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        var joinBuilder = SnakesProto.GameMessage.JoinMsg.newBuilder();
        joinBuilder.setPlayerName(playerName);
        joinBuilder.setGameName(gameName);
        joinBuilder.setRequestedRole(role);
        builder.setJoin(joinBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
