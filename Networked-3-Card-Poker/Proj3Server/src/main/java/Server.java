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

                    callback.accept("Client " + count + " connected.");
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

        ClientThread(Socket socket, int count) {
            this.connection = socket;
            this.count = count;
        }

        public void run() {
            try {
                in = new ObjectInputStream(connection.getInputStream());
                out = new ObjectOutputStream(connection.getOutputStream());
                connection.setTcpNoDelay(true);
            } catch (Exception e) {
                System.out.println("Streams not open");
            }


            // Existing logic for dealing cards and communication
            PokerInfo dealerInfo = new PokerInfo();
            serverDealer.dealDealerHand();
            dealerInfo.player.setHand(serverDealer.getHand());
            dealerInfo.isDealer = true;
            sendPokerInfoToClient(dealerInfo);

            dealerHand = serverDealer.getHand();

            PokerInfo playerInfo = new PokerInfo();
            playerInfo.player.setHand(serverDealer.dealHand());
            playerInfo.isDealer = false;
            sendPokerInfoToClient(playerInfo);

            while (true) {
                try {
                    PokerInfo pokerInfo = (PokerInfo) in.readObject();

                    if (pokerInfo.gameRes.equals("Bets Placed")) {
                        processBets(pokerInfo);
                    } else if (pokerInfo.gameRes.equals("Play") || pokerInfo.gameRes.equals("Fold")) {
                        processPlayFold(pokerInfo);
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


        private void processPlayFold(PokerInfo pokerInfo) {
            if (pokerInfo.gameRes.equals("Play")) {
                callback.accept("Client " + this.count + " chose to PLAY, they wagered: $" + pokerInfo.player.getAnteBet());
                evaluateWins(pokerInfo.player.getHand(), dealerHand, pokerInfo);
            } else if (pokerInfo.gameRes.equals("Fold")) {
                callback.accept("Client " + this.count + " chose to FOLD. They lost: $" +
                        (pokerInfo.player.getAnteBet() + pokerInfo.player.getPairPlusBet()));
            }
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
                callback.accept("Client " + this.count + " placed an Ante Bet of $" + pokerInfo.player.getAnteBet());
            }
            if (pokerInfo.pairPlusPlaced) {
                callback.accept("Client " + this.count + " placed an Pair-Plus Bet of $" + pokerInfo.player.getPairPlusBet());
            }

        }

        // process pp / ante wins
        private void evaluateWins(ArrayList<Card> playerHand, ArrayList<Card> dealerHand, PokerInfo pokerInfo) {
            int res = ThreeCardLogic.compareHands(playerHand, dealerHand);
            int ppWinnings = ThreeCardLogic.evalPPWinnings(playerHand, pokerInfo.player.getPairPlusBet());
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

            callback.accept("Client " + this.count + " got " + ppStringRes + " from their Pair-Plus, they won $" + ppWinnings);
            Player updatedPlayer = pokerInfo.player;

            String gameRes = "";
            int winnings = 0;
            if (res == 0) {
                callback.accept("Client " + this.count + " and Dealer tied!");
                gameRes = "Tie";
            } else if (res == 1) {
                callback.accept("Client " + this.count + " lost to dealer!");
                gameRes = "Dealer Win";
                winnings = -(pokerInfo.player.getAnteBet() * 2);
            } else {
                callback.accept("Client " + this.count + " won against dealer!");
                gameRes = "Player Win";
                winnings = (pokerInfo.player.getAnteBet() * 2);
            }

            updatedPlayer.updateWinnings(updatedPlayer.getTotalWinnings() + winnings + ppWinnings);

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





