package core.config.refdata.ref;

import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;

public class RefPractice extends RefBaseGame {

    @RefField(iskey = true)
    public int id; // // ID潜规则
    public String gameType;//游戏类型
    public int lv;//
    public int baseMark;//基础分
    public int min;//最小
    public int max;//最大
    public String imgName;//图片名称
    public String fontColor;//字体颜色
    public String outLineColor;//线的颜色
    public String fontSize;//字体大小
    public String outLineSize;//线的字体

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean Assert() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        // TODO Auto-generated method stub
        return true;
    }


    public void setId(int id) {
        this.id = id;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public int getLv() {
        return lv;
    }

    public void setLv(int lv) {
        this.lv = lv;
    }

    public int getBaseMark() {
        return baseMark;
    }

    public void setBaseMark(int baseMark) {
        this.baseMark = baseMark;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getOutLineColor() {
        return outLineColor;
    }

    public void setOutLineColor(String outLineColor) {
        this.outLineColor = outLineColor;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public String getOutLineSize() {
        return outLineSize;
    }

    public void setOutLineSize(String outLineSize) {
        this.outLineSize = outLineSize;
    }


}
