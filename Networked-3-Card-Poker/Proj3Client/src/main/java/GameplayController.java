import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class GameplayController {

   @FXML public Label anteBetLabel, ppBetLabel, playWagerLabel;

    @FXML
    private ImageView playerCard1, playerCard2, playerCard3, dealerCard1, dealerCard2, dealerCard3;

    @FXML
    private Label winningsLabel;

    @FXML
    private Button placeBetsButton, playButton, foldButton, summaryButton,
            nextHandButton;

    @FXML
    private Label gameInfoText;

    @FXML
    private TextArea gameActionText;

    @FXML
    private TextField anteInput, pairPlusInput;

    private ClientGUI clientGUI;
    private ObjectOutputStream out;
    private boolean cardsDealt = false;
    protected Player player;
    private Player dealer;
    boolean isWin, pushedHand;

    // Set the ClientGUI instance
    public void setClientGUI(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    // Set the output stream for sending messages
    public void setConnection(ObjectOutputStream out) {
        this.out = out;
    }

    public void updateGameActionText(String message) {
        Platform.runLater(() -> {
            gameActionText.appendText("- " + message + "\n");
        });
    }

    @FXML
    public void placeBets() {
        try {
            int pairPlusValue = Integer.parseInt(pairPlusInput.getText());
            if (pairPlusValue < 0 || (pairPlusValue < 5 || pairPlusValue > 25)) {
                if (pairPlusValue != 0) {
                    showWarning("Pair-Plus Bets must be 0, or between 5 and 25.");
                    return;
                }
            }
            player.setPairPlusBet(pairPlusValue);
            ppBetLabel.setText("Pair-Plus Bet: $" + Integer.toString(player.getPairPlusBet()));

            int anteValue;
            if (anteInput.getText().equals("") && pushedHand) {
                anteValue = player.getAnteBet();
            }
            else {
                anteValue = Integer.parseInt(anteInput.getText());
                if (anteValue < 5 || anteValue > 25) {
                    showWarning("Ante Bets must be between 5 and 25.");
                    return;
                }
            }
            player.setAnteBet(anteValue);
            anteBetLabel.setText("Ante Bet: $" + Integer.toString(player.getAnteBet()));
            pushedHand = false;

            // Append game action update with the amounts bet
            String betDetails = String.format("Bets placed: Ante bet of $%d and Pair-Plus bet of $%d", anteValue, pairPlusValue);
            updateGameActionText(betDetails);

            placeBetsButton.setDisable(true);
            playButton.setDisable(false);
            foldButton.setDisable(false);

            sendPokerInfo("Bets Placed", player.getAnteBet(), player.getPairPlusBet(), false);
            cardsDealt = true;
            findCardImages(player.getHand(), null); // Hide dealer's hand
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numeric values for bets.");
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

        int anteValue;
        if (anteInput.getText().equals("")) {
            anteValue = player.getAnteBet();
        }
        else {
            anteValue = Integer.parseInt(anteInput.getText());
        }
        playWagerLabel.setText("Play Wager: $" + Integer.toString(player.getAnteBet()));


        // Display the wager bet in the game action text
        String wagerMessage = String.format("Play Hand: Wager bet of $%d.", anteValue);
        updateGameActionText(wagerMessage);

        // Disable the play and fold buttons after playing
        playButton.setDisable(true);
        foldButton.setDisable(true);

        // Reveal both player's and dealer's cards
        cardsDealt = true;
        findCardImages(player.getHand(), dealer.getHand());

        // Show the summary button after playing the game
        summaryButton.setVisible(true);
        summaryButton.setDisable(false);

        // Send poker info with action details
        sendPokerInfo("Play", anteValue, player.getPairPlusBet(), true);
    }

    @FXML
    public void foldGame() {
        // This method is triggered when "Fold" is selected
        sendPokerInfo("Fold", 0, 0, false);

        // Disable play and fold buttons after folding
        playButton.setDisable(true);
        foldButton.setDisable(true);

        // Show only the player's cards and keep the dealer's cards hidden
        cardsDealt = true;
        findCardImages(player.getHand(), null);  // Pass null for the dealer's hand to keep it hidden

        // Show the summary button after folding
        summaryButton.setVisible(true);
        summaryButton.setDisable(false);

        isWin = false;
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
        clientGUI.showWinLoseScene(isWin, player);
    }

    // Update game state when receiving info from server
    public void updateGameState(PokerInfo pokerInfo) {
        Platform.runLater(() -> {

            if (pokerInfo.gameRes.equals("Dealer does not qualify")) {
                updateGameActionText("- Dealer does not have at least Queen high; ante wager is pushed");
                pushBetsToNextHand();
            }
            if (pokerInfo.gameRes.equals("Won Pair Plus")) {
                updateGameActionText("Player Won Pair Plus");
            } else if (pokerInfo.gameRes.equals("Lost Pair Plus")) {
                updateGameActionText("Player Lost Pair Plus");
            }

            if (pokerInfo.gameRes.equals("Player Win")) {
                isWin = true;
                updateGameActionText("Player won");

                summaryButton.setVisible(true);
                summaryButton.setDisable(false);
            }

            if (pokerInfo.gameRes.equals("Dealer Win")) {
                isWin = false;
                updateGameActionText("Dealer won");

                summaryButton.setVisible(true);
                summaryButton.setDisable(false);
            }
            if (pokerInfo.gameRes.equals("Player Fold")) {
                isWin = false;
                updateGameActionText("Player Folds");

                summaryButton.setVisible(true);
                summaryButton.setDisable(false);
            }

            if (pokerInfo.isDealer) {
                dealer = pokerInfo.player;
            } else {
                player = pokerInfo.player;
            }

            if (pokerInfo.player != null) {
                winningsLabel.setText("$" + pokerInfo.player.getTotalWinnings());
            } else {
                winningsLabel.setText("$0");
            }

            if (dealer != null && player != null) {
                if (player.getAnteBet() > 0 && pokerInfo.gameRes.equals("Bets Pushed")) {
                    setupPushingBets();
                }

                findCardImages(player.getHand(), dealer.getHand());

            }
        });
    }

    private void setupPushingBets() {

        anteInput.setDisable(true);
        placeBetsButton.setDisable(false);
        playButton.setDisable(true);
        foldButton.setDisable(true);

        ppBetLabel.setText("Pair-Plus Bet: $0");
        playWagerLabel.setText("Play Wager: $0");

        pushedHand = true;

        findCardImages(null, null);  // Pass null for the dealer's hand to keep it hidden

    }

    private void pushBetsToNextHand() {
        nextHandButton.setVisible(true);
        summaryButton.setVisible(false);
    }

    @FXML private void handleNextHand() {
        winningsLabel.setText( "$" + String.valueOf(player.getTotalWinnings()));
        updateGameActionText("Ante Bet Pushed");
        anteInput.clear();
        pairPlusInput.clear();

        // Show the back of the cards at the start of the game
        cardsDealt = false;
        findCardImages(null, null);
        playButton.setDisable(false); // Disable the play button until bets are placed
        foldButton.setDisable(false); // Disable the fold button until bets are placed
        placeBetsButton.setDisable(true);
        summaryButton.setVisible(false);
        isWin = false;

        PokerInfo nextHand = new PokerInfo();
        nextHand.player = player;
        nextHand.gameRes = "Next Hand";
        clientGUI.sendPokerInfo(nextHand);

        nextHandButton.setVisible(false);

    }

    // Reset winnings for fresh start
    public void playAgain() {
        winningsLabel.setText( "$" + String.valueOf(player.getTotalWinnings()));
        updateGameActionText("Play Again");

        anteInput.setDisable(false);
        anteInput.clear();
        pairPlusInput.clear();

        // Show the back of the cards at the start of the game
        cardsDealt = false;
        findCardImages(null, null);
        playButton.setDisable(true); // Disable the play button until bets are placed
        foldButton.setDisable(true); // Disable the fold button until bets are placed
        placeBetsButton.setDisable(false);
        summaryButton.setDisable(true);
        summaryButton.setVisible(false);
        isWin = false;

        anteBetLabel.setText("Ante Bet: $0");
        ppBetLabel.setText("Pair-Plus Bet: $0");
        playWagerLabel.setText("Play Wager: $0");


        PokerInfo playAgain = new PokerInfo();
        playAgain.player = player;
        playAgain.gameRes = "Play Again";
        clientGUI.sendPokerInfo(playAgain);
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

        try {

            // If cards have been dealt, show the player's and dealer's cards
            if (cardsDealt) {
                if (playerHand != null) {
                    playerCard1.setImage(new Image(getCardImagePath(playerHand.get(0))));
                    playerCard2.setImage(new Image(getCardImagePath(playerHand.get(1))));
                    playerCard3.setImage(new Image(getCardImagePath(playerHand.get(2))));
                }

                // Show the dealer's cards only if the player has not folded
                if (dealerHand != null && !gameInfoText.getText().equals("Player Fold")) {
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
        catch (Exception e) {}
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
    @FXML
    private void handleExit() {
        Alert exitConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
        exitConfirmation.setTitle("Exit");
        exitConfirmation.setHeaderText(null);
        exitConfirmation.setContentText("Are you sure you want to quit?");

        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        exitConfirmation.getButtonTypes().setAll(yesButton, noButton);

        exitConfirmation.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                Platform.exit();
                System.exit(0);
            } else {
                exitConfirmation.close();
            }
        });
    }

    @FXML
    private void handleFreshStart() throws IOException {

        player = new Player();


        // Reset game state and UI components
        winningsLabel.setText("$0"); // Set winnings to 0
        gameActionText.clear();
        updateGameActionText("Player reset game");
        anteInput.clear();
        anteInput.setDisable(false);
        pairPlusInput.clear();
        nextHandButton.setVisible(false);

        // Show the back of the cards at the start of the game
        cardsDealt = false;
        findCardImages(null, null);
        playButton.setDisable(true); // Disable the play button until bets are placed
        foldButton.setDisable(true); // Disable the fold button until bets are placed
        placeBetsButton.setDisable(false);
        summaryButton.setDisable(true);
        summaryButton.setVisible(false);
        isWin = false;

        anteBetLabel.setText("Ante Bet: $0");
        ppBetLabel.setText("Pair-Plus Bet: $0");
        playWagerLabel.setText("Play Wager: $0");

        // Clear the current stylesheet and reapply the original one
        Scene scene = placeBetsButton.getScene();
        scene.getStylesheets().clear(); // Remove all stylesheets
        scene.getStylesheets().add(getClass().getResource("gameplay.css").toExternalForm()); // Add the original stylesheet

        // Notify server to reset the game
        PokerInfo playAgain = new PokerInfo();
        playAgain.player = player;
        playAgain.gameRes = "Fresh Start";
        clientGUI.sendPokerInfo(playAgain);
    }

    @FXML
    private void handleNewLook() {
        String newStylePath = "newlook.css";
        Scene scene = placeBetsButton.getScene();

        // Check if the new stylesheet is already applied to avoid duplicates
        if (!scene.getStylesheets().contains(getClass().getResource(newStylePath).toExternalForm())) {
            scene.getStylesheets().add(getClass().getResource(newStylePath).toExternalForm());
        }
    }
}