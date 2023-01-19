package lab4.gamehandler;

import lab4.SnakesProto;
import lab4.utils.CoordUtils;
import lab4.utils.DirectionUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Snake implements Serializable {
    private static final int EMPTY = -1;

    private final int fieldWidth;
    private final int fieldHeight;

    @Getter private Coord head;
    @Getter private Coord tail;

    @Getter @Setter private int playerID = EMPTY;
    @Getter @Setter private SnakesProto.GameState.Snake.SnakeState state = SnakesProto.GameState.Snake.SnakeState.ALIVE;
    @Getter private SnakesProto.Direction direction;

    @Getter private final List<Coord> coordinates;

    public Snake(Coord head, Coord tail, int fieldWidth, int fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.head = head;
        this.tail = tail;
        validateInitHeadAndTail(head, tail);
        this.coordinates = new ArrayList<>();
        this.coordinates.add(head);
        this.coordinates.add(tail);
        this.direction = getCurrentDirection(head, tail);
    }

    public Snake(int playerID, List<Coord> coordinates, SnakesProto.GameState.Snake.SnakeState state, SnakesProto.Direction direction, int fieldWidth, int fieldHeight) {
        this.fieldWidth = fieldWidth;
        this.fieldHeight = fieldHeight;
        this.playerID = playerID;
        this.coordinates = new ArrayList<>(coordinates);
        this.state = state;
        this.direction = direction;
        this.head = this.coordinates.get(0);
        this.tail = this.coordinates.get(this.coordinates.size() - 1);
    }

    private void validateInitHeadAndTail(Coord head, Coord tail) {
        if (!CoordUtils.areCoordsConnected(head, tail, fieldWidth, fieldHeight)) {
            throw new IllegalArgumentException("Head and tail are not connected");
        }
    }

    private SnakesProto.Direction getCurrentDirection(Coord head, Coord tail) {
        validateInitHeadAndTail(head, tail);
        if (CoordUtils.getCoordToRight(head, fieldWidth).equals(tail)) {
            return SnakesProto.Direction.LEFT;
        }
        else if (CoordUtils.getCoordToLeft(head, fieldWidth).equals(tail)) {
            return SnakesProto.Direction.RIGHT;
        }
        else if (CoordUtils.getCoordBelow(head, fieldHeight).equals(tail)) {
            return SnakesProto.Direction.UP;
        }
        else if (CoordUtils.getCoordAbove(head, fieldHeight).equals(tail)) {
            return SnakesProto.Direction.DOWN;
        }
        throw new IllegalStateException("Cant calculate current direction");
    }

    public void makeMove(SnakesProto.Direction direction) {
        if (direction == null) {
            makeMove();
            return;
        }

        if (DirectionUtils.reverse(direction).equals(this.direction)) {
            direction = this.direction;
        }

        this.direction = direction;
        head = getNewHeadCoord(direction);
        coordinates.add(0, head);
    }

    private Coord getNewHeadCoord(SnakesProto.Direction direction) {
        return switch (direction) {
            case DOWN -> CoordUtils.getCoordBelow(head, fieldHeight);
            case UP -> CoordUtils.getCoordAbove(head, fieldHeight);
            case LEFT -> CoordUtils.getCoordToLeft(head, fieldWidth);
            case RIGHT -> CoordUtils.getCoordToRight(head, fieldWidth);
        };
    }

    public void makeMove() {
        makeMove(direction);
    }

    public void removeTail() {
        coordinates.remove(tail);
        if (coordinates.size() <= 1) {
            throw new IllegalStateException("Snake cant have less than 2 points");
        }
        tail = coordinates.get(coordinates.size() - 1);
    }

    public boolean isSnakeBody(Coord point) {
        for (int i = 1; i < coordinates.size() - 1; i++) {
            if (point.equals(coordinates.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean isSnake(Coord point) {
        return point.equals(head) || point.equals(tail) || isSnakeBody(point);
    }

    public boolean isSnakeHead(Coord point) {
        return point.equals(head);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Snake other)) {
            return false;
        }
        return coordinates.equals(other.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.coordinates);
    }
}
