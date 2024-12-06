import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientGUI extends Application {
    private static Stage primaryStage;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private GameplayController gameplayController;
    private WelcomePageController welcomePageController;
    private WinLoseController winLoseController;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Three Card Poker");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("welcome.fxml"));
        Parent root = loader.load();
        welcomePageController = loader.getController();
        welcomePageController.setClientGUI(this);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public void initializeConnection(String serverIp, int port) {
        try {
            socket = new Socket(serverIp, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            socket.setTcpNoDelay(true);

            Platform.runLater(() -> {
                try {
                    switchToGameplay();
                } catch (Exception e) {
                    showErrorAlert("Error", "Unable to switch to gameplay: " + e.getMessage());
                }
            });

            startDataListener();
        } catch (Exception e) {
            Platform.runLater(() -> {
                showErrorAlert("Connection Error", "Failed to connect: " + e.getMessage());
            });
        }
    }

    private void startDataListener() {
        new Thread(() -> {
            try {
                while (true) {
                    PokerInfo pokerInfo = (PokerInfo) in.readObject();
                    Platform.runLater(() -> {
                        if (gameplayController != null) {
                            gameplayController.updateGameState(pokerInfo);
                        }
                    });
                }
            } catch (Exception e) {
                showErrorAlert("Error", "Failed to receive server messages: " + e.getMessage());
            }
        }).start();
    }

    private void switchToGameplay() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gameplay.fxml"));
        Parent gameRoot = loader.load();

        gameplayController = loader.getController();
        gameplayController.setClientGUI(this);
        gameplayController.setConnection(out);

        primaryStage.setScene(new Scene(gameRoot, 800, 600));
    }

    public void showWinLoseScene(boolean isWin, int winnings) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("WinLoss.fxml"));
            Parent winLoseRoot = loader.load();
            winLoseController = loader.getController();

            Scene gameplayScene = primaryStage.getScene(); // Save current gameplay scene
            winLoseController.setPrimaryStage(primaryStage, gameplayController,gameplayScene);
            winLoseController.showResult(isWin, winnings);

            primaryStage.setScene(new Scene(winLoseRoot, 800, 600));
            primaryStage.setTitle("Game Over");
        } catch (IOException e) {
            System.err.println("Error switching to win/lose scene: " + e.getMessage());
        }
    }

    public void sendPokerInfo(PokerInfo pokerInfo) {
        try {

            out.writeObject(pokerInfo);

        } catch (Exception e) {
            showErrorAlert("Error", "Failed to send PokerInfo: " + e.getMessage());
        }
    }


    private void showErrorAlert(String title, String message) {
        System.err.println(title + ": " + message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}