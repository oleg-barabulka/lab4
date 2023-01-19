package lab4.utils;

import lab4.SnakesProto;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class DirectionUtils {
    public static SnakesProto.Direction reverse(SnakesProto.Direction direction) {
        if (direction == null) return null;
        return switch (direction) {
            case DOWN -> SnakesProto.Direction.UP;
            case UP -> SnakesProto.Direction.DOWN;
            case RIGHT -> SnakesProto.Direction.LEFT;
            case LEFT -> SnakesProto.Direction.RIGHT;
        };
    }
}
