package lab4.messages.messages;

import lab4.SnakesProto;
import lab4.gamehandler.Player;
import lab4.utils.PlayerUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public final class AnnouncementMessage extends Message {
    private static final int EMPTY = -1;

    @Getter private final List<SnakesProto.GameAnnouncement> games;

    public AnnouncementMessage(List<SnakesProto.GameAnnouncement> games, long messageSequence) {
        super(MessageType.ANNOUNCEMENT, messageSequence, EMPTY, EMPTY);
        this.games = games;
    }

    public AnnouncementMessage(long messageSequence) {
        super(MessageType.ANNOUNCEMENT, messageSequence, EMPTY, EMPTY);
        games = new ArrayList<>();
    }

    public void addGame(List<Player> players, SnakesProto.GameConfig config, String gameName, boolean canJoin) {
        SnakesProto.GameAnnouncement.Builder gameAnnouncement = SnakesProto.GameAnnouncement.newBuilder();
        gameAnnouncement.setGameName(gameName);
        var gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (var player : players) {
            gamePlayersBuilder.addPlayers(PlayerUtils.createPlayerForMessage(player));
        }
        gameAnnouncement.setPlayers(gamePlayersBuilder.build());
        gameAnnouncement.setConfig(config);
        gameAnnouncement.setCanJoin(canJoin);
        games.add(gameAnnouncement.build());
    }

    @Override
    public SnakesProto.GameMessage getGameMessage() {
        var builder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.AnnouncementMsg.Builder announcementBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
        for (SnakesProto.GameAnnouncement game : games) {
            announcementBuilder.addGames(game);
        }
        builder.setAnnouncement(announcementBuilder.build());
        builder.setMsgSeq(getMessageSequence());
        return builder.build();
    }
}
