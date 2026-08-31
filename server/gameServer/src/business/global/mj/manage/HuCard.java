package business.global.mj.manage;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;

import java.util.List;

public interface HuCard {

    public boolean checkHuCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType);

    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit);

    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, Object opHu);

    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType);

    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit);

    public <T> Object checkHuCardReturn(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType);

    public <T> boolean checkHuCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardType, Object opHu);

    public <T> boolean checkHuCard(AbsMJSetPos mSetPos, int cardType, Object opHu);

    public boolean checkHuCard(AbsMJSetPos mSetPos, int jin);

    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType);

    public boolean checkHuCard(AbsMJSetPos aSetPos);

    public boolean tingYouJin(AbsMJSetPos setPos);

    public List<MJCard> qiangJinHuCard(AbsMJSetPos setPos);

    public boolean doQiangJin(AbsMJSetPos mSetPos, List<MJCard> qiangJinList);

    public boolean checkHuCard(AbsMJSetPos hbSetPos, List<Integer> allCardList, String[] cardTypes, List<Integer> cardList, int lastOutCard);

    public boolean checkHuCard(AbsMJSetPos mSetPos, MJCardInit mCardInit, Integer cardType, Object opHu, Integer posID);

}
