package lab4.client.view.javafx;

import lab4.gamehandler.Player;
import javafx.scene.paint.Color;

import java.util.*;

public final class PlayerColorMapper {
    private static final Color ZOMBIE_SNAKE_COLOR = Color.BLACK;
    private static final List<Color> SNAKE_COLORS = List.of(Color.RED, Color.BLUE, Color.PURPLE);

    private final Map<Player, Color> playerColors;
    private int prevColorIndex;

    public PlayerColorMapper() {
        playerColors = new HashMap<>();
        prevColorIndex = -1;
    }

    public Optional<Color> getColor(Player player) {
        return Optional.of(playerColors.get(player));
    }

    public void addPlayer(Player player) {
        prevColorIndex = (prevColorIndex + 1) % SNAKE_COLORS.size();
        playerColors.put(player, SNAKE_COLORS.get(prevColorIndex));
    }

    public void removePlayer(Player player) {
        playerColors.remove(player);
    }

    public boolean isPlayerRegistered(Player player) {
        return playerColors.containsKey(player);
    }

    public Set<Player> getRegisteredPlayers() {
        return Collections.unmodifiableSet(playerColors.keySet());
    }

    public Color getZombieColor() {
        return ZOMBIE_SNAKE_COLOR;
    }
}
