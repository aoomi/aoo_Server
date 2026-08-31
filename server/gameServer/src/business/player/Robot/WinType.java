package business.player.Robot;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 和牌类型。
 */
public interface WinType {
    /**
     * 返回建议打出的牌，即从手牌中排除掉明显不应该打出的牌并返回。返回的列表按建议的优先级从高到低排列。
     */
    public List<Tile> getDiscardCandidates(Set<Tile> aliveTiles, Collection<Tile> candidates);

    /**
     * 一种结果是和牌的换牌方法，移除removedTiles并增加addedTiles。
     */
    public static class ChangingForWin {
        public Set<Tile> removedTiles, addedTiles;
        private int hashCode;

        public ChangingForWin(Set<Tile> removedTiles, Set<Tile> addedTiles) {
            this.removedTiles = removedTiles;
            this.addedTiles = addedTiles;
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                final int prime = 31;
                int result = 1;
                result = prime * result + addedTiles.stream().map(Tile::type).mapToInt(TileType::hashCode).sum();
                result = prime * result + removedTiles.stream().map(Tile::type).mapToInt(TileType::hashCode).sum();
                hashCode = result;
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof ChangingForWin)) {
                return false;
            }
            ChangingForWin other = (ChangingForWin) obj;
            return hashCode() == other.hashCode();
        }

        @Override
        public String toString() {
            return "ChangingForWin [removedTiles=" + removedTiles + ", addedTiles=" + addedTiles + "]";
        }

    }
}
