package lab4.client.view.javafx;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lab4.multicastreceiver.GameInfo;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public final class ActiveGameButton {
    @Getter private final String gameName;
    @Setter @Getter private NetNode masterNode;
    @Getter private final int playersCount;

    @Getter private final SnakesProto.GameConfig config;

    @Getter private final String fieldSize;
    @Getter private final String foodNumber;

    @Getter private final Button button;

    public ActiveGameButton(GameInfo gameInfo) {
        gameName = gameInfo.getGameName();
        playersCount = gameInfo.getPlayers().size();
        config = gameInfo.getConfig();
        masterNode = gameInfo.getMasterNode();
        button = new Button("Вход");
        fieldSize = config.getHeight() + "x" + config.getWidth();
        foodNumber = config.getFoodStatic() + "+" + playersCount;
        designButton();
    }

    private void designButton() {
        button.setBackground(new Background(new BackgroundFill(Color.BLUE, new CornerRadii(0), new Insets(0))));
        button.setTextFill(Color.WHITE);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ActiveGameButton other)) {
            return false;
        }
        return gameName.equals(other.gameName) && masterNode.equals(other.masterNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterNode, gameName);
    }
}
