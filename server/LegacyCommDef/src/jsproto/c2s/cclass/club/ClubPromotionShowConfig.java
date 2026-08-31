package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 亲友圈配置
 *
 * @author Administrator
 */
@Data
public class ClubPromotionShowConfig extends BaseSendMsg {
    /**
     * 基础设置
     */
    private List<Integer> showConfig = new ArrayList<>();
    /**
     * 二级菜单按钮显示
     */

    private List<Integer> showConfigSecond = new ArrayList<>();

    public ClubPromotionShowConfig(List<Integer> showConfig, List<Integer> showConfigSecond) {
        this.showConfig = showConfig;
        this.showConfigSecond = showConfigSecond;
    }

}
