package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 战绩记录对象
 *
 * @author Administrator
 */
@Data
public class ClubPlayerRoomAloneData implements Serializable {
    /**
     * 总数量
     */
    private Long total;
    /**
     * 记录列表
     */
    private List<ClubPlayerRoomAloneLogBO> logBOList;

    public ClubPlayerRoomAloneData(Long total, List<ClubPlayerRoomAloneLogBO> logBOList) {
        this.total = total;
        this.logBOList = logBOList;
    }
}
