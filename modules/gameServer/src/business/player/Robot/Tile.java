package business.player.Robot;

import business.player.Robot.TileRank.NumberRank;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Objects;
import com.ddm.server.common.semantic.ValueSemantics;

/**
 * 麻将牌。
 */
@ValueSemantics
public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final List<Tile> all;

    static {
        // 初始化所有牌
        List<Tile> allTiles = TileType.all().stream()
                .flatMap(
                        type -> IntStream.range(0, type.suit().getTileCountByType()).<Tile>mapToObj(id -> new Tile(type, id)))
                .collect(Collectors.toList());
        all = Collections.unmodifiableList(allTiles);
    }

    /**
     * 返回所有144张牌的列表。
     */
    public static List<Tile> all() {
        return all;
    }

    /**
     * 返回指定牌型的所有牌的集合。
     */
    public static Set<Tile> allOfType(TileType type) {
        return all.stream().filter(tile -> tile.type() == type).collect(Collectors.toSet());
    }

    /**
     * 返回指定牌。
     */
    public static Tile of(TileType type, int id) {
        return all.stream().filter(tile -> tile.type() == type && tile.id() == id).findAny().orElse(null);
    }

    private final TileType type;
    private final int id;// 每个牌型从0到3。

    private Tile(TileType type, int id) {
        this.type = type;
        this.id = id;
    }

    /**
     * 返回牌型。
     */
    public TileType type() {
        return type;
    }

    /**
     * 返回ID，同一牌型从0开始，通常是[0,3]。
     */
    public int id() {
        return id;
    }

    public int cardId() {
        if (type.suit().getRankClass() == NumberRank.class) {
            String str = type.suit().toString();
            if ("WAN".equals(str)) {
                return 1 * 1000 + ((NumberRank) type.rank()).number() * 100 + this.id + 1;
            } else if ("TIAO".equals(str)) {
                return 2 * 1000 + ((NumberRank) type.rank()).number() * 100 + this.id + 1;
            } else if ("BING".equals(str)) {
                return 3 * 1000 + ((NumberRank) type.rank()).number() * 100 + this.id + 1;
            }

        } else {
            String str = type.rank().toString();
            if ("DONG_FENG".equals(str)) {
                return 4100 + this.id + 1;
            } else if ("XI".equals(str)) {
                return 4200 + this.id + 1;
            } else if ("NAN".equals(str)) {
                return 4300 + this.id + 1;
            } else if ("BEI".equals(str)) {
                return 4400 + this.id + 1;
            } else if ("ZHONG".equals(str)) {
                return 4500 + this.id + 1;
            } else if ("FA".equals(str)) {
                return 4600 + this.id + 1;
            } else if ("BAI".equals(str)) {
                return 4700 + this.id + 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "[" + type + ", " + id + "]";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Tile tile && id == tile.id && type.equals(tile.type);
    }

    @Override
    public int hashCode() { return Objects.hash(type, id); }

}
