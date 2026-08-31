package jsproto.c2s.iclass.client;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CFF00_OneKeyRegAccount extends BaseSendMsg {

    @Override
    public String toString() {
        return "CFF00_OneKeyRegAccount{" +
                "Head=" + Head +
                '}';
    }
}
