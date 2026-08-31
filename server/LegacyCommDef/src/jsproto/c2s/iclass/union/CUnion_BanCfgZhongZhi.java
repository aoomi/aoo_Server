package jsproto.c2s.iclass.union;

import java.util.ArrayList;
import java.util.List;

/**
 * 赛事玩家分组
 *
 * @author zaf
 */
public class CUnion_BanCfgZhongZhi extends CUnion_Base {
    /**
     * 玩家id
     */
    private long pid;
    /**
     * 勾选id
     */
    private List<Long> configIdList = new ArrayList<>();

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }


    public List<Long> getConfigIdList() {
        return configIdList;
    }

    public void setConfigIdList(List<Long> configIdList) {
        this.configIdList = configIdList;
    }
}