import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class WinLoseController {

    @FXML
    public Button playAnotherGameButton, exitButton;
    @FXML
    private Label resultLabel;
    @FXML
    private Label winningsLabel;
    @FXML
    private Rectangle resultBackground;

    private Stage primaryStage;
    private GameplayController gameplayController;
    private Scene gameplayScene;

    // Method to set the primary stage and scene for switching
    public void setPrimaryStage(Stage primaryStage, GameplayController controller, Scene gameplayScene) {
        this.primaryStage = primaryStage;
        this.gameplayController = controller;
        this.gameplayScene = gameplayScene;
    }

    // Method to display the win/lose scene with appropriate messages
    public void showResult(boolean isWin, Player player) {
        if (isWin) {
            resultLabel.setText("You Won!");
            resultBackground.setFill(Color.GREEN);
        } else {
            resultLabel.setText("You Lost!");
            resultBackground.setFill(Color.RED);
        }

        // Update the winnings label and change the color based on the amount
        winningsLabel.setText("Total Winnings: $" + player.getTotalWinnings());
        if (player.getTotalWinnings() > 0) {
            winningsLabel.setTextFill(Color.GREEN); // Green if positive
            winningsLabel.setStyle("-fx-font-weight: bold; -fx-font-family: Impact; -fx-font-size: 20px;");

        } else if (player.getTotalWinnings() < 0) {
            winningsLabel.setTextFill(Color.RED); // Red if negative
            winningsLabel.setStyle("-fx-font-weight: bold; -fx-font-family: Impact; -fx-font-size: 20px;");

        } else {
            winningsLabel.setTextFill(Color.BLACK); // Default color for zero
            winningsLabel.setStyle("-fx-font-weight: bold; -fx-font-family: Impact; -fx-font-size: 20px;");

        }
    }

    // Handler for the "Play Again" button
    @FXML
    public void handlePlayAgain() throws IOException {
        gameplayController.playAgain();
        primaryStage.setScene(gameplayScene);
    }

    // Handler for the "Exit" button
    @FXML
    public void handleExit() {

        Platform.exit();
        System.exit(0);
    }

}




