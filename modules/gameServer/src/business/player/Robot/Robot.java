package business.player.Robot;

import com.ddm.server.common.Config;

public class Robot {

    private int pid;
    private String name;
    private String url;
    private int sex;
    private int gold;


    public Robot(int pid, String[] data) {
        this.pid = pid;
        this.name = data[0];
        this.url = data[1];
    }


    public int getPid() {
        return this.pid;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
//		return "http://www.zle.com/headImage/" + this.url;
        return Config.getRobotHeadImageUrl() + this.url;
    }

    public int getSex() {
        return this.sex;
    }

    /**
     * @return gold
     */
    public int getGold() {
        return gold;
    }


    /**
     * @param gold 要设置的 gold
     */
    public void setGold(int gold) {
        this.gold = gold;
    }

}
