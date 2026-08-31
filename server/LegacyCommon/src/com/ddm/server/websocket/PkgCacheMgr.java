package com.ddm.server.websocket;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.MessageHeader;

import java.util.LinkedList;
import java.util.Queue;


/**
 * 每个session对象的收发缓存<br>
 * <p>
 * /**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public class PkgCacheMgr {

    private final int MaxCachedCnt = 50;

    private Queue<PkgRecv> recvs = new LinkedList<>();

    public PkgCacheMgr() {
    }

    public synchronized void cacheSent(MessageHeader header, String body) {
        String key = header.event + "@" + header.sequence;
        for (PkgRecv recv : recvs) {
            if (recv.getKey().equals(key)) {
                recv.setCatchedResponse(body);
                return;
            }
        }
    }

    public synchronized void cacheSent(short opcode, short seqnum, short errcode, String errString) {
        String key = String.format("%s@%s", opcode, seqnum);
        for (PkgRecv recv : recvs) {
            if (recv.getKey().equals(key)) {
                recv.setCatchedResponse(errcode, errString);
                return;
            }
        }
    }

    public synchronized PkgRecv fetchSentOrRegist(short opcode, short seqnum) {
        String key = String.format("%s@%s", opcode, seqnum);
        for (PkgRecv recv : recvs) {
            if (recv.getKey().equals(key)) {
                return recv;
            }
        }

        recvs.add(new PkgRecv(key, opcode, seqnum));

        if (recvs.size() > MaxCachedCnt) {
            recvs.poll().onRemove();
        }
        return null;
    }

    public class PkgRecv {

        private short opcode;
        private short seqnum;
        private String codeSeq;

        private int createTime;
        private String cachedResponse;
        private short errorCode;
        private String errString;

        public PkgRecv(String codeSeq, short opcode, short seqnum) {
            this.codeSeq = codeSeq;
            this.opcode = opcode;
            this.seqnum = seqnum;
            this.createTime = CommTime.nowSecond();
        }

        public String getKey() {
            return codeSeq;
        }

        public int getCreateTime() {
            return createTime;
        }

        public String getCatchedResponse() {
            return cachedResponse;
        }

        public void setCatchedResponse(String proto) {
            this.cachedResponse = proto;
        }

        public short getCatchedErrorCode() {
            return errorCode;
        }

        public String getCatchedErrString() {
            return errString;
        }

        public void setCatchedResponse(short errcode, String errString) {
            this.errorCode = errcode;
            this.errString = errString;
        }

        public void onRemove() {
            if (cachedResponse == null && this.errString == null) {
                CommLogD.error("PkgRecv not sendBack : " + Integer.toHexString(opcode) + "@" + seqnum);
            }
        }

        @Override
        public String toString() {
            return Integer.toHexString(opcode) + "@" + seqnum;
        }
    }
}
