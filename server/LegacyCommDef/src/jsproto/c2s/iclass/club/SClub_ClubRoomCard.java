package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubRoomCardAttention;

import java.util.ArrayList;

/**
 * 俱乐部房卡变了通知
 *
 * @author zaf
 */
public class SClub_ClubRoomCard extends BaseSendMsg {

    public ArrayList<ClubRoomCardAttention> roomCardAttentions;

    public static SClub_ClubRoomCard make(ArrayList<ClubRoomCardAttention> roomCardAttentions) {
        SClub_ClubRoomCard ret = new SClub_ClubRoomCard();
        ret.roomCardAttentions = roomCardAttentions;
        return ret;
    }
}