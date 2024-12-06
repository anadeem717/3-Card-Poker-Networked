import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

public class WelcomePageController {
    @FXML
    private TextField serverIpField;

    @FXML
    private TextField portField;

    private ClientGUI clientGUI;

    public void setClientGUI(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    @FXML
    private void handleConnectToServer() {
        String serverIp = serverIpField.getText().trim();
        String portString = portField.getText().trim();

        if (serverIp.isEmpty() || portString.isEmpty()) {
            showErrorAlert("Input Error", "Please enter both IP and port.");
            return;
        }

        try {
            int port = Integer.parseInt(portString);
            if (clientGUI != null) {
                clientGUI.initializeConnection(serverIp, port);
            } else {
                showErrorAlert("Connection Error", "ClientGUI instance is not available.");
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Invalid Port Number", "Please enter a valid port number.");
        }
    }

    // Public method to show error alerts
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}