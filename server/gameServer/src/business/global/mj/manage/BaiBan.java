package business.global.mj.manage;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;

import java.util.List;

public interface BaiBan {
    public List<MJCard> baiBan(AbsMJSetPos mSetPos, int Jin, int Jin2);
}
