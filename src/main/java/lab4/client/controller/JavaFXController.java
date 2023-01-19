package lab4.client.controller;

import lab4.SnakesProto;
import lab4.client.controller.events.MoveEvent;
import lab4.client.controller.events.ServerPlayerEvent;
import lab4.client.controller.events.UserEvent;
import lab4.client.model.ClientGameHandler;
import lab4.client.controller.events.JoinGameEvent;
import lombok.RequiredArgsConstructor;
import lab4.client.view.View;

@RequiredArgsConstructor
public final class JavaFXController implements GameController {
    private final SnakesProto.GameConfig config;
    private final String playerName;
    private final ClientGameHandler clientGame;
    private final View view;

    @Override
    public void event(UserEvent userEvent) {
        switch (userEvent.getType()) {
            case NEW_GAME -> {
                view.setConfig(config);
                clientGame.startNewGame();
            }
            case JOIN_GAME -> {
                JoinGameEvent joinEvent = (JoinGameEvent) userEvent;
                view.setConfig(joinEvent.getConfig());
                clientGame.joinGame(joinEvent.getNode(), playerName);
            }
            case MOVE -> {
                MoveEvent moveEvent = (MoveEvent) userEvent;
                clientGame.handleMove(moveEvent.getDirection());
            }
            case SERVER_PLAYER -> {
                ServerPlayerEvent serverPlayerEvent = (ServerPlayerEvent) userEvent;
                clientGame.joinServerPlayer(serverPlayerEvent.getServerNetNode());
            }
            case EXIT -> clientGame.exit();
        }
    }
}
