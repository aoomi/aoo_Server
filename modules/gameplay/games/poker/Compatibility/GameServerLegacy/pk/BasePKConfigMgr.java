package business.global.pk;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Txt2Utils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * 跑得快 配置文件
 *
 * @author zaf
 */
public class BasePKConfigMgr {
    private String fileName = "PKConfig.txt";
    public static final String filePath = "conf/";
    private Map<String, String> configMap = new HashMap<String, String>();
    private int godCard = 0;//神牌模式
    private List<Integer> privateCard1; //玩家一
    private List<Integer> privateCard2; //玩家二
    private List<Integer> privateCard3; //玩家三
    private List<Integer> privateCard4; //玩家一
    private List<Integer> privateCard5; //玩家二
    private List<Integer> privateCard6; //玩家三
    private List<Integer> privateCard7; //玩家一
    private List<Integer> privateCard8; //玩家二
    private List<Integer> publicCard;//公共牌堆
    protected int cardType;

    public BasePKConfigMgr(String name) throws NumberFormatException {
        try {
            setFileName(name);
            this.configMap = Txt2Utils.txt2Map(filePath, fileName, "GBK");
            if (MapUtils.isNotEmpty(configMap) && configMap.containsKey("God_Card")) {
                this.godCard = Integer.parseInt(this.configMap.get("God_Card"));
                if (this.godCard == 1) {
                    this.cardType = this.configMap.containsKey("cardType") ? getIntValue(this.configMap.get("cardType")) : 1;
                }
                this.privateCard1 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card1"));
                this.privateCard2 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card2"));
                this.privateCard3 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card3"));
                this.privateCard4 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card4"));
                this.privateCard5 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card5"));
                this.privateCard6 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card6"));
                this.privateCard7 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card7"));
                this.privateCard8 = Txt2Utils.String2ListInteger(this.configMap.get("Private_Card8"));
                this.publicCard = Txt2Utils.String2ListInteger(this.configMap.get("publicCard"));
            }

        } catch (Exception e) {
            // TODO: handle exception
            CommLogD.error(name + "ConfigMgr alloc error msg=" + e.toString());
        }
    }

    public int getIntValue(String cardType) {
        if (cardType.startsWith("0x") || cardType.startsWith("0X")) {
            return Integer.parseInt(cardType.substring(2), 16);
        }
        return Integer.parseInt(cardType);
    }

    public void setFileName(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return;
        }
        this.fileName = String.format("PK%sConfig.txt", fileName);
    }

    public int getGodCard() {
        return godCard;
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

    public List<Integer> getPrivateCard5() {
        return privateCard5;
    }

    public List<Integer> getPrivateCard6() {
        return privateCard6;
    }

    public List<Integer> getPrivateCard7() {
        return privateCard7;
    }

    public List<Integer> getPrivateCard8() {
        return privateCard8;
    }

    public List<Integer> getPublicCard() {
        return publicCard;
    }

    public void clear() {
        if (Objects.nonNull(this.configMap)) {
            this.configMap.clear();
            this.configMap = null;
        }
        if (Objects.nonNull(this.privateCard1)) {
            this.privateCard1.clear();
            this.privateCard1 = null;
        }
        if (Objects.nonNull(this.privateCard2)) {
            this.privateCard2.clear();
            this.privateCard2 = null;
        }
        if (Objects.nonNull(this.privateCard3)) {
            this.privateCard3.clear();
            this.privateCard3 = null;
        }
        if (Objects.nonNull(this.privateCard4)) {
            this.privateCard4.clear();
            this.privateCard4 = null;
        }

        if (Objects.nonNull(this.privateCard5)) {
            this.privateCard5.clear();
            this.privateCard5 = null;
        }
        if (Objects.nonNull(this.privateCard6)) {
            this.privateCard6.clear();
            this.privateCard6 = null;
        }
        if (Objects.nonNull(this.privateCard7)) {
            this.privateCard7.clear();
            this.privateCard7 = null;
        }
        if (Objects.nonNull(this.privateCard8)) {
            this.privateCard8.clear();
            this.privateCard8 = null;
        }
        if (Objects.nonNull(this.publicCard)) {
            this.publicCard.clear();
            this.publicCard = null;
        }
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
        if (this.privateCard7 != null) {
            allPrivateCardList.add(this.privateCard7);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        if (this.privateCard8 != null) {
            allPrivateCardList.add(this.privateCard8);
        } else {
            allPrivateCardList.add(new ArrayList<Integer>());
        }
        return allPrivateCardList;
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    /**
     * 是否十进制
     *
     * @return boolean
     */
    public boolean isDecimalism() {
        return cardType == 2;
    }

    public int getCardType() {
        return cardType;
    }
}
