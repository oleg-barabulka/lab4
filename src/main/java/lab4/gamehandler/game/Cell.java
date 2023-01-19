package lab4.gamehandler.game;

import lombok.Getter;
import lombok.Setter;
import lab4.gamehandler.Coord;

public final class Cell {
    @Getter private final Coord coord;
    @Getter @Setter private CellType type;

    public Cell(int x, int y, CellType type) {
        this.coord = new Coord(x, y);
        this.type = type;
    }

    public Cell(Coord coord, CellType type) {
        this.coord = coord;
        this.type = type;
    }

    public Cell(int x, int y) {
        this(x, y, CellType.EMPTY);
    }

    public Cell(Cell cell) {
        this(cell.coord, cell.getType());
    }

    public int getX() {
        return coord.x();
    }

    public int getY() {
        return coord.y();
    }
}
