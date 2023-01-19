package lab4.utils;

import lab4.SnakesProto;
import lab4.gamehandler.Coord;
import lab4.gamehandler.Snake;
import lombok.experimental.UtilityClass;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Objects;

@UtilityClass
public final class SnakeUtils {
    private static final Logger logger = Logger.getLogger(SnakeUtils.class);

    public static SnakesProto.GameState.Snake createSnakeForMessage(Snake snake) {
        SnakesProto.GameState.Snake.Builder builder = SnakesProto.GameState.Snake.newBuilder();
        builder.setPlayerId(snake.getPlayerID());
        builder.setState(snake.getState());
        builder.setHeadDirection(snake.getDirection());
        SnakesProto.GameState.Coord.Builder cordBuilder = SnakesProto.GameState.Coord.newBuilder();
        for (Coord point: snake.getCoordinates()) {
            cordBuilder.setX(point.x());
            cordBuilder.setY(point.y());
            builder.addPoints(cordBuilder.build());
        }
        return builder.build();
    }

    public static List<Snake> getSnakeList(List<SnakesProto.GameState.Snake> snakes, SnakesProto.GameConfig config) {
        return snakes.stream().map(snake -> {
                    if (!validateSnake(snake)) {
                        logger.info("Snake doesn't have required fields");
                        return null;
                    }
                    return new Snake(
                            snake.getPlayerId(),
                            CoordUtils.getCoordList(snake.getPointsList()),
                            snake.getState(),
                            snake.getHeadDirection(),
                            config.getWidth(),
                            config.getHeight()
                    );
                })
                .filter(Objects::nonNull).toList();
    }

    private boolean validateSnake(SnakesProto.GameState.Snake snake) {
        return snake.hasPlayerId() && snake.hasState() && snake.hasHeadDirection() && snake.getPointsCount() >= 2;
    }
}
