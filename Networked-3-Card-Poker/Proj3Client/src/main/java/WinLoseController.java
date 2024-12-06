import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class WinLoseController {

    @FXML public Button playAnotherGameButton, exitButton;
    @FXML

    private Label resultLabel;
    @FXML
    private Label winningsLabel;

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
    public void showResult(boolean isWin, int winnings) {
        resultLabel.setText(isWin ? "Congratulations! You Won!" : "Sorry, You Lost.");
        winningsLabel.setText("Winnings: $" + winnings);
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
        primaryStage.close(); // Close the application
    }
}