package lab4.gamehandler.game;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lab4.gamehandler.Player;
import lab4.gamehandler.Snake;

import java.util.Map;

public interface GameHandler {
    Player registerNewPlayer(String playerName, NetNode netNode);
    void removePlayer(Player player);
    void moveAllSnakes(Map<Player, SnakesProto.Direction> playersMoves);
    Snake getSnakeByPlayer(Player player);
}
