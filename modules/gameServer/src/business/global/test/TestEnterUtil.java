package business.global.test;

import business.global.club.Club;
import business.global.club.ClubMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import com.alibaba.fastjson.JSONObject;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.UnionRoomConfigBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.UnionRoomConfigBOService;
import core.ioc.ContainerMgr;
import core.network.client2game.ClientAcceptor;
import core.network.client2game.handler.BaseHandler;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.room.CBase_EnterRoom;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试类
 *
 * @author zhujianming
 * @date 2021-12-22 17:42
 */
public class TestEnterUtil {

    public static final String xx = "jmjsmj.cjmjsmjenterroom";

    public static void enterRoom(WebSocketRequest request) throws IOException {
        enterRoom(getClubId(), getUnionId(), request);
    }

    /**
     * 得到俱乐部id
     *
     * @return int
     */
    private static long getClubId() {
        Map<Long, Club> clubMap = ClubMgr.getInstance().getClubListMgr().getClubMap();
        Comparator<Club> objectComparator = Comparator.comparingLong(y -> y.getClubListBO().getId());
        Optional<Club> first = clubMap.values().stream().sorted(objectComparator.reversed()).findFirst();
        return first.orElseThrow(() -> new IllegalStateException("test club is unavailable")).getClubListBO().getId();
    }

    public static long getUnionId() {
        Map<Long, Union> unionMap = UnionMgr.getInstance().getUnionListMgr().getUnionMap();
        Comparator<Union> objectComparator = Comparator.comparingLong(y -> y.getOwnerPlayerId());
        Optional<Union> first = unionMap.values().stream().sorted(objectComparator.reversed()).findFirst();
        return first.orElseThrow(() -> new IllegalStateException("test union is unavailable")).getUnionBO().getId();
    }

    public static void enterRoom(long clubID, long unionID, WebSocketRequest request) throws IOException {
        List<UnionRoomConfigBO> unionRoomConfigBOList = ContainerMgr.get().getComponent(UnionRoomConfigBOService.class).findAll(Restrictions.eq("unionId", unionID));
        UnionRoomConfigBO unionRoomConfigBO = unionRoomConfigBOList.get(unionRoomConfigBOList.size() - 1);
        BaseRoomConfigure baseRoomConfigure = JSONObject.parseObject(unionRoomConfigBO.getGameConfig(), BaseRoomConfigure.class);
        String roomKey = baseRoomConfigure.getUnionRoomCfg().getRoomKey();
        CBase_EnterRoom requestPIDAdd = new CBase_EnterRoom();
        requestPIDAdd.setClubId(clubID);
        requestPIDAdd.setPosID(-1);
        requestPIDAdd.setRoomKey(roomKey);
        requestPIDAdd.setExistQuickJoin(false);
        BaseHandler handler = (BaseHandler) ClientAcceptor.getInstance().getHandle(xx);
        handler.handle(request, JSONObject.toJSONString(requestPIDAdd));
    }
}
