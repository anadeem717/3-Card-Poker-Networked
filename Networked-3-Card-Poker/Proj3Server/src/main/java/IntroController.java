import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class IntroController {

    @FXML
    private TextField portInput; // Text field for entering the port number

    // Start the server when the button is clicked
    public void startServer() {
        try {
            // Parse the port number from the input field
            int portNumber = Integer.parseInt(portInput.getText());

            // Switch to the Dashboard scene and pass the port number
            ServerGUI.switchToDashboard(portNumber);

        } catch (NumberFormatException e) {
            // If the port number is invalid, show an error alert
            showAlert("Invalid Port Number", "Please enter a valid port number.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Show an alert with a given title and message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
