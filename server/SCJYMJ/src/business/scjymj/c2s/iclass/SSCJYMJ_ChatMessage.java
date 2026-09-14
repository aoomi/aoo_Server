package business.scjymj.c2s.iclass;

import cenum.ChatType;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.utils.CommTime;
import core.network.proto.ChatMessage;

public class SSCJYMJ_ChatMessage extends ChatMessage {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static SSCJYMJ_ChatMessage make(long pid, String name, String content, ChatType type, long toCId, int quickID) {
        SSCJYMJ_ChatMessage ret = new SSCJYMJ_ChatMessage();
        ret.setType(ChatType.values()[type.ordinal()]);
        ret.setSenderPid(pid);
        ret.setSenderName(name);
        // 将敏感字替换成 “*”
        ret.setMessage(SensitiveWordMgr.getInstance().replaceSensitiveWord(content, 1, "*"));
        ret.setContent(content);
        ret.setSendTime(CommTime.nowSecond());
        ret.setReceivePid(toCId);
        ret.setQuickID(quickID);
        return ret;
    }
}
