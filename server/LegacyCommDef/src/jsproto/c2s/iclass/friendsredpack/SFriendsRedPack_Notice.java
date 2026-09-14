package jsproto.c2s.iclass.friendsredpack;

import cenum.FriendsHelpUnfoldRedPackEnum.TargetType;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player.ShortPlayer;


/**
 * 帮拆红包通知
 *
 * @author Administrator
 */
public class SFriendsRedPack_Notice extends BaseSendMsg {
    public ShortPlayer player;
    public int value;
    public TargetType targetType;
    public boolean isHu;

    public static SFriendsRedPack_Notice make(ShortPlayer player, int value, TargetType targetType, boolean isHu) {
        SFriendsRedPack_Notice ret = new SFriendsRedPack_Notice();
        ret.player = player;
        ret.value = value;
        ret.targetType = targetType;
        ret.isHu = isHu;
        return ret;


    }
}