package lab4.client.view;

import lab4.SnakesProto;
import lab4.gamehandler.GameState;
import lab4.multicastreceiver.GameInfo;

import java.util.Collection;

public interface View {
    void setConfig(SnakesProto.GameConfig gameConfig);
    void updateCurrentGame(GameState state);
    void updateGameList(Collection<GameInfo> gameInfos);
}
