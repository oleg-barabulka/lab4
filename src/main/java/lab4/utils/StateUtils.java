package lab4.utils;

import lab4.SnakesProto;
import lab4.config.ConfigProperty;
import lab4.gamehandler.GameState;
import lab4.gamehandler.Player;
import lab4.gamehandler.Coord;
import lab4.gamehandler.Snake;
import lombok.experimental.UtilityClass;
import org.apache.log4j.Logger;

@UtilityClass
public final class StateUtils {
    private static final Logger logger = Logger.getLogger(StateUtils.class);

    public static GameState getStateFromMessage(SnakesProto.GameState state) {
        if (!validateGameState(state)) {
            logger.info("Game state doesn't have required fields");
            return null;
        }
        return new GameState(
                CoordUtils.getCoordList(state.getFoodsList()),
                PlayerUtils.getPlayerList(state.getPlayers().getPlayersList()),
                SnakeUtils.getSnakeList(state.getSnakesList(), ConfigProperty.getConfig()),
                ConfigProperty.getConfig(),
                state.getStateOrder()
        );
    }

    private static boolean validateGameState(SnakesProto.GameState state) {
        return state.hasStateOrder() && state.hasPlayers();
    }

    public static SnakesProto.GameState createStateForMessage(GameState state) {
        var builder = SnakesProto.GameState.newBuilder();
        builder.setStateOrder(state.getStateID());
        for (Snake snake: state.getSnakes()) {
            builder.addSnakes(SnakeUtils.createSnakeForMessage(snake));
        }
        var coordBuilder = SnakesProto.GameState.Coord.newBuilder();
        for (Coord fruit: state.getFoods()) {
            coordBuilder.setX(fruit.x());
            coordBuilder.setY(fruit.y());
            builder.addFoods(coordBuilder.build());
        }
        var playersBuilder = SnakesProto.GamePlayers.newBuilder();
        for (Player player: state.getActivePlayers()) {
            playersBuilder.addPlayers(PlayerUtils.createPlayerForMessage(player));
        }
        builder.setPlayers(playersBuilder.build());
        return builder.build();
    }

    public static String getMasterNameFromState(GameState state) {
        for (Player player: state.getActivePlayers()) {
            if (player.getRole().equals(SnakesProto.NodeRole.MASTER)) {
                return player.getName();
            }
        }
        return null;
    }

    public static Player getDeputyFromState(GameState state) {
        for (Player player: state.getActivePlayers()) {
            if (player.getRole().equals(SnakesProto.NodeRole.DEPUTY)) {
                return player;
            }
        }
        return null;
    }
}