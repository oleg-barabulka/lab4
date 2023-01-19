package lab4.master;

import lab4.gamehandler.GameState;

public interface MasterHandler {
    void update(GameState state);
    int getPort();
    void stop();
}
