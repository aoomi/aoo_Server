package jsproto.c2s.cclass.share;

import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/17 10:55
 * @description 共享游戏类型
 */
@Data
public class ShareGameType {
    // ID
    private int id;
    // 名称
    private String name;
    // 类型
    private int type;
}
