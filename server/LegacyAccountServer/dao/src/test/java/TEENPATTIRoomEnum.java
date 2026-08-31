package business.global.pk.teenpatti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 青年帕蒂房间枚举
 */
public class TEENPATTIRoomEnum {

    /**
     * 牌型枚举
     *
     * @author Administrator
     * @date 2021/06/04
     */
    public enum CardType{
        Not(0),
        GaoKa(1),
        PeiDui(2),
        TongHua(3),
        ShunZi(4),
        TongHuaShun(5),
        Zuji(6);
        int value;

        CardType(int value) {
            this.value = value;
        }

        public static CardType getType(String name){
            if("杂牌".equals(name)){
                return CardType.GaoKa;
            }else if("对子".equals(name)){
                return CardType.PeiDui;
            }else if("金花".equals(name)){
                return CardType.TongHua;
            }else if("顺子".equals(name)){
                return CardType.ShunZi;
            }else if("顺金".equals(name)){
                return CardType.TongHuaShun;
            }else if("豹子".equals(name)){
                return CardType.Zuji;
            }
            return CardType.Not;
        }
    }


    /**
     * 百叶窗
     *
     * @author Administrator
     * @date 2021/06/04
     */
    public enum BaiYeChuang{
        Four(4),;
        int value;

        BaiYeChuang(int value) {
            this.value = value;
        }

        public static int getValue(int index){
            for(BaiYeChuang item:BaiYeChuang.values()){
                if(item.ordinal() == index){
                    return item.value;
                }
            }
            return 10000;
        }
    }

    /**
     * 启动金额
     *
     * @author Administrator
     * @date 2021/06/04
     */
    public enum QiDongJinE {
        One(0.1,1.0),
        Two(0.5,21.0),
        Three(1.0,40.0),
        Four(5.0,100.0),
        Five(10.0,200.0),
        Six(20.0,400.0),
        Seven(50.0,800.0),
        Eight(100.0,1600.0),
        Nine(200.0,3200.0),
        Ten(300.0,4800.0),
        ;
        double value1;
        double value2;

        QiDongJinE(double value1, double value2) {
            this.value1 = value1;
            this.value2 = value2;
        }

        public static double getQD(int index){
            for(QiDongJinE item: QiDongJinE.values()){
                if(item.ordinal() == index){
                    return item.value1;
                }
            }
            return 0;
        }

        public static int getIndex(double difen){
            for(QiDongJinE item: QiDongJinE.values()){
                if(item.value1 == difen){
                    return item.ordinal();
                }
            }
            return 0;
        }

        public static double getMK(int index){
            for(QiDongJinE item: QiDongJinE.values()){
                if(item.ordinal() == index){
                    return item.value2;
                }
            }
            return 0;
        }
    }

}
