import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class GameplayController {

    @FXML
    private ImageView playerCard1, playerCard2, playerCard3, dealerCard1, dealerCard2, dealerCard3;

    @FXML
    private Label winningsLabel;

    @FXML
    private Button placeBetsButton, playButton, foldButton, summaryButton; // Added playButton and foldButton

    @FXML
    private Text gameInfoText;

    @FXML
    private TextField anteInput, pairPlusInput;

    private ClientGUI clientGUI;
    private ObjectOutputStream out;
    private boolean cardsDealt = false;
    private Player player;
    private Player dealer;
    boolean isWin;

    // Set the ClientGUI instance
    public void setClientGUI(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    // Set the output stream for sending messages
    public void setConnection(ObjectOutputStream out) {
        this.out = out;
    }

    @FXML
    public void placeBets() {
        try {
            int pairPlusValue = Integer.parseInt(pairPlusInput.getText());
            if ((pairPlusValue < 5 || pairPlusValue > 25)) {
                if (pairPlusValue != 0) {
                    showWarning("Pair-Plus Bets must be 0, or between 5 and 25");
                    return;
                }
            }
            player.setPairPlusBet(pairPlusValue);

            int anteValue = Integer.parseInt(anteInput.getText());
            if (anteValue < 5 || anteValue > 25) {
                showWarning("Ante Bets must be between 5 and 25");
                return;
            }
            player.setAnteBet(anteValue);

            placeBetsButton.setDisable(true);
            playButton.setDisable(false);
            foldButton.setDisable(false); // Enable play and fold buttons

            // Send the poker information when bets are placed
            sendPokerInfo("Bets Placed", player.getAnteBet(), player.getPairPlusBet(), false);

            // Reveal only the player's cards when bets are placed
            cardsDealt = true;
            findCardImages(player.getHand(), null);  // Pass null for the dealer's hand to keep it hidden
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid numeric value for Pair Plus.");
        }
    }

    // Helper method to show a warning alert
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Bet");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void playGame() {
        // This method is triggered when "Play" is selected
        sendPokerInfo("Play", player.getAnteBet(), player.getPairPlusBet(), true);

        playButton.setDisable(true);
        foldButton.setDisable(true);

        // Reveal both player's and dealer's cards
        cardsDealt = true;
        findCardImages(player.getHand(), dealer.getHand());
    }

    @FXML
    public void foldGame() {
        // This method is triggered when "Fold" is selected
        sendPokerInfo("Fold", 0, 0, false);

        // Disable play and fold buttons after folding
        playButton.setDisable(true);
        foldButton.setDisable(true);
    }

    // Generic method to send poker info
    private void sendPokerInfo(String action, int anteValue, int pairPlusValue, boolean isPlay) {
        try {
            PokerInfo pokerInfo = new PokerInfo(
                    player,                // Player instance
                    action,   // Game action description
                    isPlay ? "Play" : "Fold", // Play or fold action
                    anteValue > 0,         // Ante placed (true if anteValue > 0)
                    pairPlusValue > 0,     // Pair Plus placed (true if pairPlusValue > 0)
                    false                  // Is dealer (always false for player actions)
            );

            clientGUI.sendPokerInfo(pokerInfo); // Send PokerInfo to the server
        } catch (Exception e) {
            showAlert("Error", "An error occurred while sending PokerInfo to the server: " + e.getMessage());
        }
    }

    @FXML public void handleSummaryButton() {
        clientGUI.showWinLoseScene(isWin, player.getTotalWinnings());
    }

    // Update game state when receiving info from server
    public void updateGameState(PokerInfo pokerInfo) {
        Platform.runLater(() -> {
            if (pokerInfo.player != null) {
                winningsLabel.setText("$" + pokerInfo.player.getTotalWinnings());
            } else {
                winningsLabel.setText("$0");
            }

            gameInfoText.setText(pokerInfo.gameRes);

            if (pokerInfo.gameRes.equals("Player Win") ||
                    pokerInfo.gameRes.equals("Dealer Win") ||
                    pokerInfo.gameRes.equals("Tie")) {
                // Show the win/lose scene when the game is over
                isWin = pokerInfo.gameRes.equals("Player Win");

                summaryButton.setVisible(true);
                summaryButton.setDisable(false);

            }

            // Card display logic
            if (pokerInfo.isDealer) {
                dealer = pokerInfo.player;
            } else {
                player = pokerInfo.player;
            }

            if (dealer != null && player != null) {
                findCardImages(player.getHand(), dealer.getHand());
            }
        });
    }

    // Reset winnings for fresh start
    public void resetWinnings() {
        winningsLabel.setText("$0");
        gameInfoText.setText("Game Reset");
        anteInput.clear();
        pairPlusInput.clear();

        // Show the back of the cards at the start of the game
        cardsDealt = false;
        findCardImages(null, null);
        playButton.setDisable(true); // Disable the play button until bets are placed
        foldButton.setDisable(true); // Disable the fold button until bets are placed
    }

    // Show an alert with a given title and message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void findCardImages(ArrayList<Card> playerHand, ArrayList<Card> dealerHand) {
        String backImagePath = "/CardImages/back.png";

        // If cards have been dealt, show the player's and dealer's cards
        if (cardsDealt) {
            if (playerHand != null) {
                playerCard1.setImage(new Image(getCardImagePath(playerHand.get(0))));
                playerCard2.setImage(new Image(getCardImagePath(playerHand.get(1))));
                playerCard3.setImage(new Image(getCardImagePath(playerHand.get(2))));
            }

            if (dealerHand != null) {
                dealerCard1.setImage(new Image(getCardImagePath(dealerHand.get(0))));
                dealerCard2.setImage(new Image(getCardImagePath(dealerHand.get(1))));
                dealerCard3.setImage(new Image(getCardImagePath(dealerHand.get(2))));
            }
        } else {
            // Show the back of all cards if no bets have been placed yet
            dealerCard1.setImage(new Image(backImagePath));
            dealerCard2.setImage(new Image(backImagePath));
            dealerCard3.setImage(new Image(backImagePath));

            playerCard1.setImage(new Image(backImagePath));
            playerCard2.setImage(new Image(backImagePath));
            playerCard3.setImage(new Image(backImagePath));
        }
    }

    private String getCardImagePath(Card card) {
        String valueString = getCardValueString(card.value);
        String suitString = "";

        switch (card.suit) {
            case 'C':
                suitString = "clubs";
                break;
            case 'D':
                suitString = "diamonds";
                break;
            case 'H':
                suitString = "hearts";
                break;
            case 'S':
                suitString = "spades";
                break;
            default:
                suitString = "unknown";
        }

        return "/CardImages/" + valueString + "_of_" + suitString + ".png";
    }

    private String getCardValueString(int value) {
        switch (value) {
            case 11:
                return "jack";
            case 12:
                return "queen";
            case 13:
                return "king";
            case 14:
                return "ace";
            default:
                return String.valueOf(value);
        }
    }
}