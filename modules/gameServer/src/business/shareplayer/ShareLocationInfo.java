package business.shareplayer;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家位置信息对象
 */
@Data
public class ShareLocationInfo {
    private int pos = -1;
    private double Latitude; //纬度
    private double Longitude; //精度
    private boolean isGetError = true;//获取是否失败
    private String Address = "";//地址
    private long pid;
    private int updateTime = 0;
}
