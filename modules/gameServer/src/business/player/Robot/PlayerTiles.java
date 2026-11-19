package business.player.Robot;

import java.util.HashSet;
import java.util.Set;

/**
 * 一个玩家的牌。
 */
public class PlayerTiles {
    /**
     * 手中的牌。
     */
    protected Set<Tile> aliveTiles = new HashSet<>();

    /**
     * 吃碰杠。
     */

    public PlayerTiles() {
        super();
    }

    public Set<Tile> getAliveTiles() {
        return aliveTiles;
    }

    public void setAliveTiles(Set<Tile> aliveTiles) {
        this.aliveTiles = aliveTiles;
    }

}