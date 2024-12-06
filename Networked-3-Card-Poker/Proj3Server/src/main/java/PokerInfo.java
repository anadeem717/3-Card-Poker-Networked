import java.io.Serializable;
import java.util.ArrayList;

public class PokerInfo implements Serializable {

    Player player;
    String gameRes;
    String playOrFold;
    boolean antePlaced;
    boolean pairPlusPlaced;
    boolean isDealer;


    PokerInfo() {
        this.player = new Player();
        this.gameRes = "";
        this.playOrFold = "";
        this.antePlaced = false;
        this.pairPlusPlaced = false;
        this.isDealer = false;
    }

    PokerInfo(Player player, String gameRes, String playOrFold,
              boolean antePlaced, boolean pairPlusPlaced, boolean isDealer) {

        this.player = player;
        this.gameRes = gameRes;
        this.playOrFold = playOrFold;
        this.antePlaced = antePlaced;
        this.pairPlusPlaced = pairPlusPlaced;
        this.isDealer = false;
    }

    @Override
    public String toString() {
        return "PokerInfo{" +
                "player=" + player +
                ", gameRes='" + gameRes + '\'' +
                ", playOrFold='" + playOrFold + '\'' +
                ", antePlaced=" + antePlaced +
                ", pairPlusPlaced=" + pairPlusPlaced +
                '}';
    }

}
