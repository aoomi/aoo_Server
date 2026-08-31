package jsproto.c2s.cclass.arena;

/**
 * 比赛场排名项
 *
 * @author Administrator
 */
public class ArenaRankItem {
    private int rankInt = 0;
    private String arenaName;
    private String name;

    public int getRankInt() {
        return rankInt;
    }

    public void setRankInt(int rankInt) {
        this.rankInt = rankInt;
    }

    public String getArenaName() {
        return arenaName;
    }

    public void setArenaName(String arenaName) {
        this.arenaName = arenaName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArenaRankItem(int rankInt, String arenaName, String name) {
        super();
        this.rankInt = rankInt;
        this.arenaName = arenaName;
        this.name = name;
    }

    @Override
    public String toString() {
        return "ArenaRankItem [rankInt=" + rankInt + ", arenaName=" + arenaName + ", name=" + name + "]";
    }


}
