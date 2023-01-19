package lab4.client.model;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;

public interface ClientGameHandler {
    void startNewGame();
    void joinGame(NetNode gameOwner, String playerName);
    void joinServerPlayer(NetNode serverNetNode);
    void handleMove(SnakesProto.Direction direction);
    void exit();
}
