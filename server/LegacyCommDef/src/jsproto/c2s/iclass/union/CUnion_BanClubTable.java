package jsproto.c2s.iclass.union;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 禁止游戏操作
 * @author zaf
 *
 */
@Data
public class CUnion_BanClubTable extends CUnion_Base {
    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 禁止入桌列表
     */
    private List<Long> banConfigIdList=new ArrayList<>();

}