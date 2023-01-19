package lab4.gamehandler;

import lab4.SnakesProto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
public final class GameState implements Serializable {
    @Getter private final List<Coord> foods;
    @Getter private final List<Player> activePlayers;
    @Getter private final List<Snake> snakes;
    @Getter private final SnakesProto.GameConfig gameConfig;
    @Getter private final int stateID;
}
