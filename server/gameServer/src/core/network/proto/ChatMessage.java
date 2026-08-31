package core.network.proto;

import cenum.ChatType;
import jsproto.c2s.cclass.BaseSendMsg;

public class ChatMessage extends BaseSendMsg {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    long id;
    ChatType type;
    long senderPid;
    String senderName;
    int senderLv;
    int senderVipLv;
    int senderIcon;
    String message;
    String content;
    int sendTime;
    long receivePid;
    String receiveName;
    int quickID;

    public ChatMessage() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public long getSenderPid() {
        return senderPid;
    }

    public void setSenderPid(long senderPid) {
        this.senderPid = senderPid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getSenderLv() {
        return senderLv;
    }

    public void setSenderLv(int senderLv) {
        this.senderLv = senderLv;
    }

    public int getSenderVipLv() {
        return senderVipLv;
    }

    public void setSenderVipLv(int senderVipLv) {
        this.senderVipLv = senderVipLv;
    }

    public int getSenderIcon() {
        return senderIcon;
    }

    public void setSenderIcon(int senderIcon) {
        this.senderIcon = senderIcon;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSendTime() {
        return sendTime;
    }

    public void setSendTime(int sendTime) {
        this.sendTime = sendTime;
    }

    public int getQuickID() {
        return quickID;
    }

    public void setQuickID(int quickID) {
        this.quickID = quickID;
    }

    public long getReceivePid() {
        return receivePid;
    }

    public void setReceivePid(long receivePid) {
        this.receivePid = receivePid;
    }

    public String getReceiveName() {
        return receiveName;
    }

    public void setReceiveName(String receiveName) {
        this.receiveName = receiveName;
    }

}
