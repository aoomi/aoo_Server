package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.Player;
import lombok.Data;

@Data
public class UnionClubPlayerInfo {
    /**
     * 玩家信息
     */
    private Player.ShortPlayer shortPlayer;

    /**
     * 上级玩家信息
     */
    private Player.ShortPlayer upShortPlayer;

    /**
     * 是否禁止游戏
     */
    private boolean isUnionBanGame;
    /**
     * 竞技点
     */
    private double sportsPoint;
    /**
     * 管理员权限
     */
    private int minister;

    /**
     * 淘汰分
     */
    private double eliminatePoint;

    public UnionClubPlayerInfo(Player.ShortPlayer shortPlayer, Player.ShortPlayer upShortPlayer, boolean isUnionBanGame, double sportsPoint, int minister, double eliminatePoint) {
        this.shortPlayer = shortPlayer;
        this.upShortPlayer = upShortPlayer;
        this.isUnionBanGame = isUnionBanGame;
        this.sportsPoint = sportsPoint;
        this.minister = minister;
        this.eliminatePoint = eliminatePoint;
    }
}
