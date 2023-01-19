package lab4;

import lab4.client.controller.JavaFXController;
import lab4.client.model.ClientGame;
import lab4.client.view.javafx.GameView;
import lab4.config.ConfigProperty;
import lab4.multicastreceiver.MulticastReceiver;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public final class JavaFXStarter extends Application {
    private static final Logger logger = Logger.getLogger(JavaFXStarter.class);

    private static final String GAME_VIEW_FXML_PATH = "gameView.fxml";
    private static final String MULTICAST_ADDRESS = "239.192.0.4";
    private static final int MULTICAST_PORT = 9192;

    @Setter private static String playerName;
    @Setter private static NetworkInterface networkInterface;

    private MulticastReceiver multicastReceiver;
    private ClientGame netGame;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        SnakesProto.GameConfig config = ConfigProperty.getConfig();

        try {
            InetSocketAddress multicastInfo = new InetSocketAddress(InetAddress.getByName(MULTICAST_ADDRESS), MULTICAST_PORT);

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(JavaFXStarter.class.getClassLoader().getResource(GAME_VIEW_FXML_PATH));
            SplitPane root = loader.load();

            GameView view = loader.getController();
            netGame = new ClientGame(config, playerName, view, multicastInfo, networkInterface);
            JavaFXController gameController = new JavaFXController(config, playerName, netGame, view);

            multicastReceiver = new MulticastReceiver(multicastInfo, view, networkInterface);
            multicastReceiver.start();

            view.setStage(stage);
            view.setGameController(gameController);

            stage.setTitle(playerName);
            stage.setScene(new Scene(root));
            stage.sizeToScene();
            stage.show();
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    @Override
    public void stop() {
        if (multicastReceiver != null) multicastReceiver.stop();
        if (netGame != null) netGame.exit();
        System.exit(0);
    }
}
