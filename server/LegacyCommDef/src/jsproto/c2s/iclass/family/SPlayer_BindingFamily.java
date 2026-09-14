package jsproto.c2s.iclass.family;

import cenum.FamilyEnum.BindingFamilyEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.family.PlayerBindingFamily;

/**
 * 返回绑定工会
 *
 * @author Huaxing
 */
public class SPlayer_BindingFamily extends BaseSendMsg {
    public long pid;
    public BindingFamilyEnum bindingEnum;
    public PlayerBindingFamily playerBindingFamily;

    public static SPlayer_BindingFamily make(long pid, BindingFamilyEnum bindingEnum, PlayerBindingFamily playerBindingFamily) {
        SPlayer_BindingFamily ret = new SPlayer_BindingFamily();
        ret.pid = pid;
        ret.bindingEnum = bindingEnum;
        ret.playerBindingFamily = playerBindingFamily;
        return ret;
    }

}
