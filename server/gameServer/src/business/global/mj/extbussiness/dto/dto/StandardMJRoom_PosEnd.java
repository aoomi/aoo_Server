package business.global.mj.extbussiness.dto;

import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJRoom_PosEnd;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StandardMJRoom_PosEnd extends BaseMJRoom_PosEnd {
    /**
     * 胡分
     */
    private Integer huPoint;
    /**
     * 杠分
     */
    private Integer gangFen;
    /**
     * 显示用(杠分实时算)
     **/
    private Integer huFen;
    private Double sportsPointTemp;
    //听
    private boolean isTing;
    /**
     * 定缺类型
     */
    private OpType dingQue = OpType.Not;

    public Integer piao;
    public Integer pao;
    public Integer bao;
    public Integer mai;

    /**
     * 中码列表
     */
    public List<Integer> zhongList;

}
