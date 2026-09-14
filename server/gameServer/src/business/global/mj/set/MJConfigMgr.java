package business.global.mj.set;

import com.ddm.server.common.utils.Txt2Utils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 麻将神牌——配置文件
 * 只有内部测试才能使用
 */
@Data
public class MJConfigMgr {
    private String fileName = "MJConfig.txt";
    public static final String filePath = "conf/";
    private String name;
    private Map<String, String> configMap = new HashMap<String, String>();
    private List<Integer> privateCard1; //玩家一
    private List<Integer> privateCard2; //玩家二
    private List<Integer> privateCard3; //玩家三
    private List<Integer> privateCard4; //玩家四
    private List<Integer> privateCard5; //玩家五
    private List<Integer> privateCard6; //玩家六
    private int godCard = 0;
    private double basisRadix = 5D;//#基础基数
    private double loseRadix = 0D;//#输分基数
    private double maxRadix = 0D;//#最大上线
    private List<Integer> jinList;//#金牌列表
    private List<Integer> moCardList;//#摸牌列表
    private Integer handCard;//#首牌
    private Integer haiDiLaoCard;// # 海底捞牌

    public MJConfigMgr(String name) {
        this.setFileName(name);
        this.configMap = Txt2Utils.txt2Map(filePath, fileName, "GBK");
        if (MapUtils.isNotEmpty(configMap) && configMap.containsKey("God_Card")) {
            this.godCard = Integer.parseInt(this.configMap.get("God_Card"));
            this.basisRadix = Double.parseDouble(this.configMap.get("BasisRadix"));
            this.loseRadix = Double.parseDouble(this.configMap.get("LoseRadix"));
            this.maxRadix = Double.parseDouble(this.configMap.get("MaxRadix"));
            this.jinList = Txt2Utils.String2ListInteger(this.configMap.get("JinList"));
            this.privateCard1 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card1"));
            this.privateCard2 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card2"));
            this.privateCard3 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card3"));
            this.privateCard4 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card4"));
            this.privateCard5 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card5"));
            this.privateCard6 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card6"));
            this.moCardList = Txt2Utils.String2ListInteger(this.configMap.get("moCardList"));
            this.handCard = this.configMap.containsKey("handCard") ? Integer.parseInt(this.configMap.get("handCard")) : 0;
            this.haiDiLaoCard = this.configMap.containsKey("haiDiLaoCard") ? Integer.parseInt(this.configMap.get("haiDiLaoCard")) : null;
        }
    }

    public Integer getHandCard() {
        return handCard;
    }

    public void setFileName(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return;
        }
        this.fileName = String.format("MJ%sConfig.txt", fileName);
    }

    public List<List<Integer>> getAllPrivateCard() {
        List<List<Integer>> allPrivateCardList = new ArrayList<List<Integer>>();
        if (this.privateCard1 != null) {
            allPrivateCardList.add(this.privateCard1);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard2 != null) {
            allPrivateCardList.add(this.privateCard2);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard3 != null) {
            allPrivateCardList.add(this.privateCard3);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard4 != null) {
            allPrivateCardList.add(this.privateCard4);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard5 != null) {
            allPrivateCardList.add(this.privateCard5);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard6 != null) {
            allPrivateCardList.add(this.privateCard6);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        return allPrivateCardList;
    }

    public List<Integer> getPrivateCard1() {
        return privateCard1;
    }

    public List<Integer> getPrivateCard2() {
        return privateCard2;
    }

    public List<Integer> getPrivateCard3() {
        return privateCard3;
    }

    public List<Integer> getPrivateCard4() {
        return privateCard4;
    }

    public List<Integer> getJinList() {
        return jinList;
    }

    public List<Integer> getMoCardList() {
        return moCardList;
    }

    public Integer getHaiDiLaoCard() {
        return haiDiLaoCard;
    }

    /**
     * 获取第几个金
     *
     * @param index 下标从 0...99
     * @return
     */
    public int getJin(int index) {
        if (CollectionUtils.isNotEmpty(this.jinList) && (this.jinList.size() - 1) >= index) {
            return this.jinList.get(index);
        } else {
            return 0;
        }
    }


    public int getGodCard() {
        return godCard;
    }

    public double getBasisRadix() {
        return basisRadix;
    }

    public double getLoseRadix() {
        return loseRadix;
    }

    public double getMaxRadix() {
        return maxRadix;
    }

    public void clear() {
        if (Objects.nonNull(configMap)) {
            this.configMap.clear();
            this.configMap = null;
        }
        if (Objects.nonNull(privateCard1)) {
            this.privateCard1.clear();
            this.privateCard1 = null;
        }
        if (Objects.nonNull(privateCard2)) {
            this.privateCard2.clear();
            this.privateCard2 = null;
        }

        if (Objects.nonNull(privateCard3)) {
            this.privateCard3.clear();
            this.privateCard3 = null;
        }
        if (Objects.nonNull(privateCard4)) {
            this.privateCard4.clear();
            this.privateCard4 = null;
        }

        if (Objects.nonNull(privateCard5)) {
            this.privateCard5.clear();
            this.privateCard5 = null;
        }
        if (Objects.nonNull(privateCard6)) {
            this.privateCard6.clear();
            this.privateCard6 = null;
        }
    }

}
