import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Server {

    private ArrayList<ClientThread> clients = new ArrayList<>();
    private TheServer server;
    private Consumer<Serializable> callback;
    private DashboardController controller; // Reference to the DashboardController
    int count = 1;
    Dealer serverDealer;

    public Server(Consumer<Serializable> call) {
        callback = call;
    }

    // Set the controller to allow communication
    public void setController(DashboardController controller) {
        this.controller = controller;
    }

    public void start(int portNumber) {
        server = new TheServer(portNumber);
        server.start();
    }

    public void stopServer() {
        server.stopServer();
    }

    class TheServer extends Thread {
        private int portNumber;

        public TheServer(int portNumber) {
            this.portNumber = portNumber;
        }

        public void run() {
            serverDealer = new Dealer();

            try (ServerSocket mysocket = new ServerSocket(portNumber)) {
                callback.accept("Server started on port: " + portNumber);

                while (true) {
                    ClientThread clientThread = new ClientThread(mysocket.accept(), count);

                    synchronized (clients) {
                        clients.add(clientThread);
                    }

                    callback.accept("Client #" + count + " connected.");
                    updateClientCount(); // Update the label
                    count++;
                    clientThread.start();
                }
            } catch (Exception e) {
                callback.accept("Server socket did not launch");
            }
        }

        public void stopServer() {
            if (server != null) {
                System.exit(0); // Close the application
            }
        }
    }

    public class ClientThread extends Thread {

        Socket connection;
        int count;
        ObjectInputStream in;
        ObjectOutputStream out;
        ArrayList<Card> dealerHand;
        Dealer dealer;

        ClientThread(Socket socket, int count) {
            this.connection = socket;
            this.count = count;
            this.dealer = new Dealer();
        }

        public void run() {
            try {
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                connection.setTcpNoDelay(true);
            } catch (Exception e) {
                System.out.println("Streams not open");
            }

            // shuffle new deck
            dealer.shuffleNewDeck();

            // Existing logic for dealing cards and communication
            PokerInfo dealerInfo = new PokerInfo();
            dealer.dealDealerHand();
            dealerInfo.player.setHand(dealer.getHand());
            dealerInfo.isDealer = true;
            sendPokerInfoToClient(dealerInfo);

            dealerHand = dealer.getHand();

            PokerInfo playerInfo = new PokerInfo();
            playerInfo.player.setHand(dealer.dealHand());
            playerInfo.isDealer = false;
            sendPokerInfoToClient(playerInfo);

            while (true) {
                try {
                    PokerInfo pokerInfo = (PokerInfo) in.readObject();

                    if (pokerInfo.gameRes.equals("Bets Placed")) {
                        processBets(pokerInfo);
                    }

                    else if (pokerInfo.gameRes.equals("Play") || pokerInfo.gameRes.equals("Fold")) {
                        processPlayFold(pokerInfo);
                    }

                    else if (pokerInfo.gameRes.equals("Play Again")) {
                        callback.accept("Client #" + count + " is playing another hand!");
                        playAgain(pokerInfo);
                    }

                    else if (pokerInfo.gameRes.equals("Next Hand")) {
                        callback.accept("Client #" + count + " started the next hand!");

                        // shuffle new deck
                        dealer.shuffleNewDeck();

                        PokerInfo newDealerInfo = new PokerInfo();
                        dealer.dealDealerHand();
                        dealerHand = dealer.getHand();
                        newDealerInfo.player.setHand(dealer.getHand());
                        newDealerInfo.isDealer = true;
                        sendPokerInfoToClient(newDealerInfo);

                        PokerInfo newPlayerInfo = new PokerInfo();
                        newPlayerInfo.player = pokerInfo.player;
                        newPlayerInfo.gameRes = "Bets Pushed";
                        newPlayerInfo.player.setHand(dealer.dealHand());
                        newPlayerInfo.isDealer = false;
                        sendPokerInfoToClient(newPlayerInfo);
                    }

                    else if (pokerInfo.gameRes.equals("Fresh Start")) {
                        callback.accept("Client #" + count + " has reset their game!");
                        playAgain(pokerInfo);
                    }

                } catch (Exception e) {
                    callback.accept("Client #" + count + " has left the server!");
                    synchronized (clients) {
                        clients.remove(this);
                    }
                    updateClientCount();
                    break;
                }
            }
        }

        private void playAgain(PokerInfo pokerInfo) {
            // shuffle new deck
            dealer.shuffleNewDeck();

            PokerInfo newDealerInfo = new PokerInfo();
            dealer.dealDealerHand();
            dealerHand = dealer.getHand();
            newDealerInfo.player.setHand(dealer.getHand());
            newDealerInfo.isDealer = true;
            sendPokerInfoToClient(newDealerInfo);

            PokerInfo newPlayerInfo = new PokerInfo();
            newPlayerInfo.player = pokerInfo.player;
            newPlayerInfo.player.setHand(dealer.dealHand());
            newPlayerInfo.isDealer = false;
            sendPokerInfoToClient(newPlayerInfo);
        }


        private void processPlayFold(PokerInfo pokerInfo) {
            if (pokerInfo.gameRes.equals("Play")) {

                PokerInfo updatedPlayer = evaluatePPWinnings(pokerInfo.player.getHand(), pokerInfo);

                if (ThreeCardLogic.handQualifies(dealerHand)) {
                    callback.accept("Client #" + this.count + " chose to PLAY, they wagered: $" + updatedPlayer.player.getAnteBet());
                    evaluateWins(updatedPlayer.player.getHand(), dealerHand, updatedPlayer);
                }
                else { // hand does not qualify
                    PokerInfo dealerNotQualify = new PokerInfo();
                    dealerNotQualify.gameRes = "Dealer does not qualify";
                    dealerNotQualify.player = pokerInfo.player;
                    dealerNotQualify.isDealer = false;
                    callback.accept("Client #" + this.count + " dealer hand does not qualify, bets pushed");
                    sendPokerInfoToClient(dealerNotQualify);
                }
            }

            // folded
            else if (pokerInfo.gameRes.equals("Fold")) {
                callback.accept("Client #" + this.count + " chose to FOLD. They lost: $" +
                        (pokerInfo.player.getAnteBet() + pokerInfo.player.getPairPlusBet()));

                pokerInfo.gameRes = "Player Fold";
                pokerInfo.player.updateWinnings(-(pokerInfo.player.getAnteBet() + pokerInfo.player.getPairPlusBet()));
                pokerInfo.isDealer = false; // Set to false if the response is for the player
                sendPokerInfoToClient(pokerInfo);
            }
        }

        private PokerInfo evaluatePPWinnings(ArrayList<Card> playerHand, PokerInfo pokerInfo) {

            int ppWinnings = 0;

            if (pokerInfo.player.getPairPlusBet() > 0) {
                ppWinnings = ThreeCardLogic.evalPPWinnings(playerHand, pokerInfo.player.getPairPlusBet());
                int ppRes = ThreeCardLogic.evalHand(playerHand);

                String ppStringRes = "";
                if (ppRes == 1) ppStringRes = "a STRAIGHT FLUSH";
                else if (ppRes == 2) ppStringRes = "a 3 OF A KIND";
                else if (ppRes == 3) ppStringRes = "a STRAIGHT";
                else if (ppRes == 4) ppStringRes = "a FLUSH";
                else if (ppRes == 5) ppStringRes = "a PAIR";
                else {
                    ppStringRes = "NOTHING";
                }

                if (ppRes < 6) {
                    PokerInfo wonPP = new PokerInfo();
                    wonPP.gameRes = "Won Pair Plus";
                    wonPP.player.updateWinnings(ppWinnings);
                    sendPokerInfoToClient(wonPP);
                }
                else {
                    PokerInfo wonPP = new PokerInfo();
                    wonPP.gameRes = "Lost Pair Plus";
                    wonPP.player.updateWinnings(-pokerInfo.player.getPairPlusBet());
                    sendPokerInfoToClient(wonPP);
                }

                callback.accept("Client #" + this.count + " got " + ppStringRes +
                        " from their Pair-Plus, they won $" + ppWinnings +
                        " (profit = $" + (ppWinnings - pokerInfo.player.getPairPlusBet()) + ")");
            }

            PokerInfo updatedPlayer = new PokerInfo();
            updatedPlayer = pokerInfo;
            updatedPlayer.player.updateWinnings((ppWinnings - pokerInfo.player.getPairPlusBet()));
            updatedPlayer.player.setPairPlusBet(0);

            return updatedPlayer;
        }

        public void sendPokerInfoToClient(PokerInfo pokerInfo) {
            try {
                out.writeObject(pokerInfo);
                out.flush();  // Ensure the data is sent immediately
            } catch (Exception e) {
                callback.accept("Error sending PokerInfo to client: " + e.getMessage());
            }
        }

        private void processBets(PokerInfo pokerInfo) {

            if (pokerInfo.antePlaced) {
                callback.accept("Client #" + this.count + " placed an Ante Bet of $" + pokerInfo.player.getAnteBet());
            }
            if (pokerInfo.pairPlusPlaced) {
                callback.accept("Client #" + this.count + " placed an Pair-Plus Bet of $" + pokerInfo.player.getPairPlusBet());
            }

        }

        // process pp / ante wins
        private void evaluateWins(ArrayList<Card> playerHand, ArrayList<Card> dealerHand, PokerInfo pokerInfo) {


            Player updatedPlayer = pokerInfo.player;

            int res = ThreeCardLogic.compareHands(dealerHand, playerHand);

            String gameRes = "";
            int winnings = 0;

            if (res == 0) {
                callback.accept("Client #" + this.count + " and Dealer tied!");
                gameRes = "Tie";
            }

            else if (res == 1) {
                callback.accept("Client #" + this.count + " lost to dealer!");
                gameRes = "Dealer Win";
                winnings = -(pokerInfo.player.getAnteBet() * 2);
            }

            else if (res == 2) {
                winnings = (pokerInfo.player.getAnteBet());
                callback.accept("Client #" + this.count + " won against dealer! " +
                        " (profit = $" + (pokerInfo.player.getAnteBet() * 2) + ")");
                gameRes = "Player Win";
            }

            updatedPlayer.updateWinnings(winnings);

            // send updated game info
            PokerInfo newInfo = new PokerInfo();
            newInfo.player = updatedPlayer;
            newInfo.gameRes = gameRes;
            sendPokerInfoToClient(newInfo);

        }

    }

    // Update the client count label in the controller
    private void updateClientCount() {
        int connectedClients;

        synchronized (clients) {
            connectedClients = clients.size();
        }
        if (controller != null) {
            controller.updateClientCountLabel(connectedClients);
        }
    }
}