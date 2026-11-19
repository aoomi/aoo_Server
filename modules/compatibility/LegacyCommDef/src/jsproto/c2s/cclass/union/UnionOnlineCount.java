package jsproto.c2s.cclass.union;

import lombok.Data;

import java.io.Serializable;

@Data
public class UnionOnlineCount implements Serializable {
    /**
     * 统计人数
     */
    private int count;

    public UnionOnlineCount(int count) {
        this.count = count;
    }

    public UnionOnlineCount() {
    }
}
