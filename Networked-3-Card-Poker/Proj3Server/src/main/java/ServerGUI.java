import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerGUI extends Application {

    static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage; // Store reference to primary stage
        switchToIntro(); // Start with the Intro scene
        primaryStage.setTitle("Poker Server");
        primaryStage.show(); // Show the primary stage
    }

    // Switch to the Intro scene
    public static void switchToIntro() throws Exception {
        FXMLLoader loader = new FXMLLoader(ServerGUI.class.getResource("Intro.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root, 400, 200)); // Set size of the scene
    }

    // Switch to the Dashboard scene and pass the port number
    public static void switchToDashboard(int portNumber) throws Exception {
        FXMLLoader loader = new FXMLLoader(ServerGUI.class.getResource("Dashboard.fxml"));
        Parent root = loader.load();

        // Get the DashboardController and call startServer with the port number
        DashboardController controller = loader.getController();
        controller.startServer(portNumber);

        primaryStage.setScene(new Scene(root, 600, 400)); // Set size of the scene
    }

    // Main entry point for the application
    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}
