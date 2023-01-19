package lab4.utils;

import lab4.SnakesProto;
import lab4.gamehandler.Coord;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class CoordUtils {
    public static List<Coord> getCoordList(List<SnakesProto.GameState.Coord> cordList) {
        return cordList.stream().map(cord -> new Coord(cord.getX(), cord.getY())).toList();
    }

    public static Coord getCoordAbove(Coord coord, int fieldHeight) {
        return new Coord(coord.x(), getCoordinate(coord.y() - 1, fieldHeight));
    }

    public static Coord getCoordBelow(Coord coord, int fieldHeight) {
        return new Coord(coord.x(), getCoordinate(coord.y() + 1, fieldHeight));
    }

    public static Coord getCoordToRight(Coord coord, int fieldWidth) {
        return new Coord(getCoordinate(coord.x() + 1, fieldWidth), coord.y());
    }

    public static Coord getCoordToLeft(Coord coord, int fieldWidth) {
        return new Coord(getCoordinate(coord.x() - 1, fieldWidth), coord.y());
    }

    public static boolean areCoordsConnected(Coord c1, Coord c2, int fieldWidth, int fieldHeight) {
        return getConnectedCoords(c1, fieldWidth, fieldHeight).contains(c2);
    }

    private static List<Coord> getConnectedCoords(Coord coord, int fieldWidth, int fieldHeight) {
        return List.of(
                CoordUtils.getCoordAbove(coord, fieldHeight),
                CoordUtils.getCoordBelow(coord, fieldHeight),
                CoordUtils.getCoordToLeft(coord, fieldWidth),
                CoordUtils.getCoordToRight(coord, fieldWidth)
        );
    }

    private static int getCoordinate(int coordinate, int coordinateLimit) {
        if (coordinate >= coordinateLimit) {
            return coordinate % coordinateLimit;
        }
        else if (coordinate < 0) {
            return coordinateLimit + coordinate;
        }
        return coordinate;
    }
}
