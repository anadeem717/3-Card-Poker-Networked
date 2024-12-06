import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    ListView<String> serverLog; // List view to display server logs

    @FXML
    private Label clientCountLabel; // Label to show the number of connected clients

    private Server server; // The Server instance to handle server actions

    // Set the server instance and pass this controller as a reference
    public void setServer(Server server) {
        this.server = server;
        this.server.setController(this); // Link the server to this controller
    }

    // Start the server with the given port number
    public void startServer(int portNumber) {
        try {
            // Create the server instance and set up the callback to update the log
            server = new Server(data -> Platform.runLater(() -> serverLog.getItems().add(data.toString())));

            // Link the controller to the server
            setServer(server);

            // Start the server with the given port number
            server.start(portNumber);

            // Log the server start and update the UI
            clientCountLabel.setText("Clients Connected: 0");

        } catch (Exception e) {
            // Handle any other errors
            showAlert("Server Error", "An error occurred while starting the server.");
        }
    }

    // Update the client count label safely on the JavaFX Application Thread
    public void updateClientCountLabel(int count) {
        Platform.runLater(() -> clientCountLabel.setText("Clients Connected: " + count));
    }

    // Stop the server when the button is clicked
    public void stopServer() {
        if (server != null) {
            server.stopServer();
            Platform.exit(); // Close the application
        }
    }

    // Show an alert with a given title and message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

