package lab4.gamehandler.game;

import lombok.Getter;
import lab4.gamehandler.Coord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class GameField {
    private final List<Cell> field;
    private final List<Cell> emptyCells;
    @Getter private final int width;
    @Getter private final int height;

    private static final Random random = new Random();

    public GameField(int width, int height) {
        field = new ArrayList<>(width * height);
        emptyCells = new ArrayList<>(width * height);
        for (int row = 0; row < height; row++){
            for (int col = 0; col < width; col++){
                Cell cell = new Cell(col, row);
                field.add(cell);
                emptyCells.add(cell);
            }
        }
        this.width = width;
        this.height = height;
    }

    public Cell get(int row, int col){
        return new Cell(getCell(row, col));
    }

    private Cell getCell(int row, int col){
        int y = (row < 0) ? height + row : row % height;
        int x = (col < 0) ? width + col : col % width;
        return field.get(y * width + x);
    }

    public void set(int row, int col, CellType type) {
        Cell cell = getCell(row, col);
        if (type.equals(CellType.EMPTY)) {
            if (!cell.getType().equals(CellType.EMPTY)) {
                emptyCells.add(cell);
            }
        }
        else {
            emptyCells.remove(cell);
        }
        cell.setType(type);
    }

    public void set(Coord point, CellType type) {
        set(point.y(), point.x(), type);
    }

    public int getEmptyCellsCount() {
        return emptyCells.size();
    }

    Optional<Cell> findCenterOfSquareWithOutSnake(int squareSize) {
        return field.stream().filter(cell -> isSquareWithoutSnake(cell, squareSize)).findFirst();
    }

    private boolean isSquareWithoutSnake(Cell squareCenter, int squareSize) {
        final int centerOffset = squareSize / 2;
        for (int yCenterOffset = -centerOffset; yCenterOffset <= centerOffset; yCenterOffset++) {
            for (int xCenterOffset = -centerOffset; xCenterOffset <= centerOffset; xCenterOffset++) {
                Cell cell = getCell(squareCenter.getY() + yCenterOffset, squareCenter.getX() + xCenterOffset);
                if (cell.getType().equals(CellType.SNAKE)) return false;
            }
        }
        return true;
    }

    public Optional<Cell> findRandomEmptyCell() {
        return emptyCells.isEmpty() ?
                Optional.empty() : Optional.of(emptyCells.get(random.nextInt(emptyCells.size() - 1)));
    }
}
