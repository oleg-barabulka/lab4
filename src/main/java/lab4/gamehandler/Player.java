package lab4.gamehandler;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@AllArgsConstructor
@RequiredArgsConstructor
public final class Player implements Serializable {
    @Getter private final String name;
    @Getter private final int id;
    @Getter private final NetNode netNode;
    @Getter @Setter private SnakesProto.NodeRole role = SnakesProto.NodeRole.NORMAL;
    @Getter @Setter private int score = 0;

    public void incrementScore() {
        score++;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Player other)) {
            return false;
        }
        return name.equals(other.name) && netNode.equals(other.netNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, netNode);
    }
}
