package business.global.mj.cdxzmj;

public class CDXZMJRoomEnum {
    /**
     * 可选玩法：过胡加番、最后4张牌必胡、下雨、反赔、对对胡2番、巴倒烫、换三张；
     */
    public enum KeXuanWanFa {
        GUO_HU_JIA_FAN,
        XIA_YU,
        FAN_PEI,
        DUI_DUI_HU_2FAN,
        BA_DAO_TANG,
        HUANG_SAN_ZHANG,
        ZUI_HOU_SI_ZHANG_BI_HU,
    }




    public enum CDXZMJGameRoomConfigEnum {

        /**
         * 房间内切换人数
         */
        FangJianQieHuanRenShu,
        /**
         * 自动准备
         */
        ZiDongZhunBei,
        /**
         * 小局托管解散
         */
        TuoGuanJieSan,
        /**
         * 解散次数不超过5次
         */
        JieSanCishu5,
        /**
         * 小局托管解散
         */
        TuoGuanJieSan2,
        /**
         * 解散次数不超过3次
         */
        JieSanCishu3,
    }
}		
