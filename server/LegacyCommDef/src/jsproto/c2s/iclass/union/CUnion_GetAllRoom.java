package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 获取赛事房间列表
 *
 * @author zaf
 */
@Data
public class CUnion_GetAllRoom extends CUnion_Base {
    /**
     * 第几页
     */
    private int pageNum;
    /**
     * 查询的房间列表
     */
    private List<Long> roomKey = new ArrayList<>();

}