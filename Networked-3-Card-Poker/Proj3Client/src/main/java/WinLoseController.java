import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class WinLoseController {

    @FXML
    private Label resultLabel;
    @FXML
    private Label winningsLabel;

    private Stage primaryStage;
    private Scene gameplayScene;

    // Method to set the primary stage and scene for switching
    public void setPrimaryStage(Stage primaryStage, Scene gameplayScene) {
        this.primaryStage = primaryStage;
        this.gameplayScene = gameplayScene;
    }

    // Method to display the win/lose scene with appropriate messages
    public void showResult(boolean isWin, int winnings) {
        resultLabel.setText(isWin ? "Congratulations! You Won!" : "Sorry, You Lost.");
        winningsLabel.setText("Winnings: $" + winnings);
    }

    // Handler for the "Play Again" button
    @FXML
    public void handlePlayAgain() {
        primaryStage.setScene(gameplayScene); // Switch back to gameplay
    }

    // Handler for the "Exit" button
    @FXML
    public void handleExit() {
        primaryStage.close(); // Close the application
    }
}