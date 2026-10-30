package jsproto.c2s.iclass.friendsredpack;

import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 帮拆红包信息
 *
 * @param <T>
 * @author Administrator
 */
public class SFriendsRedPack_Info<T> extends BaseSendMsg {
    public TargetType targetType;
    public T data;

    public static <T> SFriendsRedPack_Info make(TargetType targetType, T data) {
        SFriendsRedPack_Info ret = new SFriendsRedPack_Info();
        ret.targetType = targetType;
        ret.data = data;
        return ret;


    }
}