package core.dispatch.event.room;

import business.global.club.ClubMember;
import business.global.club.ClubMgr;
import business.global.shareclub.ShareClubMemberMgr;
import business.global.shareclub.SharePromotionSectionMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import cenum.ConstEnum;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubMemberRelationBOService;
import core.db.service.clarkGame.PromotionLevelRoomConfigScorePercentBOService;
import core.db.service.clarkGame.UnionRoomConfigScorePercentBOService;
import core.db.service.clarkGame.UnionShareSectionBOService;
import core.ioc.ContainerMgr;
import core.logger.flow.FlowLogger;
import jsproto.c2s.cclass.club.PromotionShareSectionItem;
import jsproto.c2s.cclass.club.QueryUidOrPuidItem;
import jsproto.c2s.cclass.club.SharePromotionSection;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.cclass.union.UnionShareSectionItem;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class RoomPromotionShare implements BaseExecutor {
    /**
     * 房间比赛分消耗
     */
    private double roomSportsPointConsume;

    /**
     * 房间比赛分消耗
     */
    private double roomSportsPointConsumePrizePool;
    /**
     * 赛事id
     */
    private long unionId;
    /**
     * 每个玩家对应的亲友圈id
     */
    private Map<Long, Long> pidMap = new HashMap<>();//key 值为pid value 为clubid
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间号
     */
    private int roomKey;
    /**
     * 配置Id
     */
    private long configId;
    /**
     * 房间id
     */
    private long roomId;
    /**
     * 每个亲友圈对应的玩家id列表
     */
    private Map<Long, List<Long>> clubMap = new HashMap<>();//key 值为clubid value 为pid
    /**
     * ID
     */
    private int Id;
    /**
     * 玩法地区分类统计ID，不参与身份、资产或路由
     */
    private int classificationRegionId;
    /**
     * 奖金池
     */
    private double prizePool;
    /**
     * 收益类型
     */
    private int sourceType = 2;
    /**
     * 时间
     */
    private String dateTime;

    /**
     * 时间中至
     * 6.0-6.0的时间
     */
    private String dateTimeZhongZhi;
    /**
     * 对局数
     */
    private int setCount;
    /**
     * 房间配置
     */
    private String dataJsonCfg;
    /**
     * 消耗钻石数
     */
    private int consumeValue;
    /**
     * 游戏类型
     */
    private int gameId;
    /**
     * 盟主所获收益
     */
    private double unionCreateIncome;
    /**
     * 区间分成id
     */
    private long sectionId;
    /**
     * 是否分成到保险箱
     */
    private boolean caseSportsFlag;
    //百分比计算控制值
    private double scorePercentCalc;
    /**
     * 亲友圈圈主分成值
     * key 圈主id
     * value 圈主获得的值
     */
    private Map<Long, Double> clubOwnerProfit = new HashMap<>();
    /**
     * 联赛类型
     */
    private UnionDefine.UNION_TYPE unionType = UnionDefine.UNION_TYPE.NORMAL;


    public RoomPromotionShare(double roomSportsPointConsume, long unionId, Map<Long, Long> pidMap, String roomName, int roomKey, long configId, long roomId, int id, int classificationRegionId, Map<Long, List<Long>> clubMap, double prizePool, String dateTime, String dataJsonCfg, int consumeValue, int setCount, int gameId, String dateTimeZhongZhi) {
        this.roomSportsPointConsume = roomSportsPointConsume;
        this.unionId = unionId;
        this.pidMap = pidMap;
        this.roomName = roomName;
        this.roomKey = roomKey;
        this.configId = configId;
        this.roomId = roomId;
        Id = id;
        this.classificationRegionId = classificationRegionId;
        this.clubMap = clubMap;
        this.prizePool = prizePool;
        this.dateTime = dateTime;
        this.setCount = setCount;
        this.dataJsonCfg = dataJsonCfg;
        this.consumeValue = consumeValue;
        this.gameId = gameId;
        this.dateTimeZhongZhi = dateTimeZhongZhi;
    }

    @Override
    public void invoke() {
        //计算值初始化
        unionCreateIncome = 0;
        // 房间消耗疲劳
        if (this.getRoomSportsPointConsume() <= 0D) {
            // 没有房间消耗
            return;
        }
        this.checkCaseSpointFlag();
        // 分成收益值 = 房间消耗比赛分 - 奖金池分数
        this.setRoomSportsPointConsumePrizePool(CommMath.subDouble(this.getRoomSportsPointConsume(), this.getPrizePool()));
        // 亲友圈分成收益
        // 	如果奖金池大于等于所收的房费，则不分成；
        double sumValue = getPrizePool() >= getRoomSportsPointConsume() ? 0D : this.clubSportsPointProfit();
        // 获取亲友圈成员信息
        ClubMember clubMemberUnionCreate;
        if (Config.isShare()) {
            clubMemberUnionCreate = ShareClubMemberMgr.getInstance().getClubMember(UnionMgr.getInstance().getUnionMemberMgr().findCreateClubMemberId(this.getUnionId()));
        } else {
            clubMemberUnionCreate = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(UnionMgr.getInstance().getUnionMemberMgr().findCreateClubMemberId(this.getUnionId()));
        }
        if (Objects.nonNull(clubMemberUnionCreate)) {
            // 盟主获取的收益
            saveUnionSportsPointProfitUnion(clubMemberUnionCreate, this.getUnionId(), this.getRoomSportsPointConsume(), this.getSourceType(), this.getRoomName(), this.getRoomKey(), getId(), this.getClassificationRegionId(), ConstEnum.ResOpType.Gain, this.getRoomId(), clubOwnerProfit);
            // 盟主分成出去的收益
            saveUnionSportsPointProfitUnion(clubMemberUnionCreate, this.getUnionId(), -sumValue, this.getSourceType(), this.getRoomName(), this.getRoomKey(), this.getId(), this.getClassificationRegionId(), ConstEnum.ResOpType.Lose, this.getRoomId(), clubOwnerProfit);
            // 奖金池与玩法经营
            double prizePool = getPrizePool() >= getRoomSportsPointConsume() ? getRoomSportsPointConsume() : getPrizePool();
            FlowLogger.roomConfigPrizePoolLog(getDateTime(), getConfigId(), getRoomId(), getSetCount(), 1, getConsumeValue(), getUnionId(), getRoomName(), getDataJsonCfg(), prizePool, getGameId(), this.getRoomSportsPointConsume() - sumValue + unionCreateIncome, getRoomSportsPointConsume());
            if (UnionDefine.UNION_TYPE.ZhongZhi.equals(this.getUnionType())) {
                FlowLogger.roomConfigPrizePoolLogZhongZhi(getDateTimeZhongZhi(), getConfigId(), getRoomId(), getSetCount(), 1, getConsumeValue(), getUnionId(), getRoomName(), getDataJsonCfg(), prizePool, getGameId(), this.getRoomSportsPointConsume() - sumValue + unionCreateIncome, getRoomSportsPointConsume());
            }
        } else {
            //添加日志 报空
            CommLogD.error("clubMemberUnionCreate  is null :{},this.getRoomSportsPointConsume():{}sumValue:{}", this.toString(), this.getRoomSportsPointConsume(), sumValue);
        }
    }

    /**
     * 亲友圈分成收益
     *
     * @return
     */
    private double clubSportsPointProfit() {
        double sumValue = 0D;
        ClubMember clubMember = null;
        //当前房间 每个人的贡献的竞技点
        double perConsume = 0D;
        ClubMember clubMemberUnionCreate;
        if (Config.isShare()) {
            clubMemberUnionCreate = ShareClubMemberMgr.getInstance().getClubMember(UnionMgr.getInstance().getUnionMemberMgr().findCreateClubMemberId(this.getUnionId()));
        } else {
            clubMemberUnionCreate = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(UnionMgr.getInstance().getUnionMemberMgr().findCreateClubMemberId(this.getUnionId()));
        }
        long unionCreatePid = Objects.isNull(clubMemberUnionCreate) ? -1 : clubMemberUnionCreate.getClubMemberBO().getPlayerID();
        for (Map.Entry<Long, List<Long>> map : clubMap.entrySet()) {
            if (Config.isShare()) {
                clubMember = ShareClubMemberMgr.getInstance().findCreate(map.getKey());
            } else {
                clubMember = ClubMgr.getInstance().getClubMemberMgr().findCreate(map.getKey());
            }

            if (Objects.nonNull(clubMember)) {
                // 盟主分给圈主的   先计算走  然后再算推广员之间的分成
                double value = 0D;//分成到圈主身上的 圈主进行推广员一层一层的分成
                //区间分成单独抽出来做
                if (clubMember.getClubMemberBO().getShareType() == UnionDefine.UNION_SHARE_TYPE.SECTION.ordinal()) {
                    value = clubMemberPromotionSection(map.getKey(), map.getValue(), clubMember, perConsume, unionCreatePid);
                    // 亲友圈圈主们一共获得的收益
                    sumValue = CommMath.addDouble(sumValue, value);
                    clubOwnerProfit.put(clubMember.getClubMemberBO().getPlayerID(), -value);
                    clubMember.getClubMemberBO().saveUnionSportsPointProfitClub(unionId, value, 3, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId);
                    continue;
                }
                if (clubMember.getClubMemberBO().getShareType() == UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal()) {
                    // 盟主分给圈主的 固定
                    // 当前亲友圈圈主获取的竞技点收益
                    // 分成比例
                    double scorePercent = ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findScorePercen(unionId, clubMember.getClubID(), configId, clubMember.getClubMemberBO().getShareType(), clubMember.getClubMemberBO().getShareFixedValue());
                    perConsume = scorePercent;
                    value = CommMath.mul(scorePercent, map.getValue().size());//固定分成
                } else if (clubMember.getClubMemberBO().getShareType() == UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal()) {
                    // 盟主分给圈主的 百分比
                    double scorePercent = CommMath.divThreePoint(ContainerMgr.get().getComponent(UnionRoomConfigScorePercentBOService.class).findScorePercen(unionId, clubMember.getClubID(), configId, clubMember.getClubMemberBO().getShareType(), clubMember.getClubMemberBO().getShareValue()), 100);//存在
                    // 数据库的是乘以一百后的  所以计算的时候除以100
                    // 分成比例
                    perConsume = CommMath.mul(scorePercent, CommMath.div(roomSportsPointConsumePrizePool, pidMap.size()));
                    value = CommMath.mul(perConsume, map.getValue().size());//乘以每人  得出圈主获得的总共的竞技点数
                }
                // 亲友圈圈主们一共获得的收益
                sumValue = CommMath.addDouble(sumValue, value);
                clubOwnerProfit.put(clubMember.getClubMemberBO().getPlayerID(), -value);
                // 亲友圈成员推广员分成
                this.clubMemberPromotion(map.getKey(), map.getValue(), clubMember, perConsume, unionCreatePid);
                if (Objects.nonNull(clubMember)) {
                    //圈主获得的分成加上去 sourceType设置3  方便后续一周记录统计
                    clubMember.getClubMemberBO().saveUnionSportsPointProfitClub(unionId, value, 3, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId);
                }
            } else {
                //添加日志 判断是否亲友圈 圈住id 为0
                CommLogD.error("RoomPromotionShare clubCreateId is null one " + map.getKey());
            }
        }
        return sumValue;
    }

    /**
     * 检查是否有开启分成走保险箱功能
     */
    public void checkCaseSpointFlag() {
        Union union;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }
        if (UnionDefine.UNION_CASE_STATUS.OPEN.ordinal() == union.getUnionBO().getShareStatus()) {
            this.setCaseSportsFlag(true);
        }
        this.setUnionType(UnionDefine.UNION_TYPE.valueOf(union.getUnionBO().getUnionType()));
    }

    /**
     * 区间分成计算
     *
     * @param clubId
     * @param clubMemberPidList
     * @param clubCreate
     * @param perConsume
     */
    public double clubMemberPromotionSection(long clubId, List<Long> clubMemberPidList, ClubMember clubCreate, double perConsume, long unionCreatePid) {
        double sumValue = 0D;//盟主给圈主的多少
        Union union;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }

        ClubMember unionCreate;
        if (Config.isShare()) {
            unionCreate = ShareClubMemberMgr.getInstance().getClubMember(union.getUnionBO().getClubId(), union.getUnionBO().getOwnerId());
        } else {
            unionCreate = ClubMgr.getInstance().getClubMemberMgr().getClubMember(union.getUnionBO().getClubId(), union.getUnionBO().getOwnerId());
        }
        List<UnionShareSectionItem> unionShareSectionItems = ((UnionShareSectionBOService) ContainerMgr.get().getComponent(UnionShareSectionBOService.class)).findAllE(Restrictions.eq("unionId", unionId), UnionShareSectionItem.class, UnionShareSectionItem.getItemsName());
        if (CollectionUtils.isEmpty(unionShareSectionItems)) {
            return sumValue;
        }
        double personTotalValueCalc = 0;
        for (UnionShareSectionItem con : unionShareSectionItems) {
            if (con.getEndFlag() == 1) {
                if (roomSportsPointConsumePrizePool > con.getBeginValue()) {
                    this.sectionId = con.getId();
                    personTotalValueCalc = CommMath.div(con.getBeginValue(), pidMap.size());
                }
            } else {
                if (roomSportsPointConsumePrizePool > con.getBeginValue() && roomSportsPointConsumePrizePool <= con.getEndValue()) {
                    this.sectionId = con.getId();
                    personTotalValueCalc = CommMath.div(con.getEndValue(), pidMap.size());
                }
            }
        }
        for (Long pid : clubMemberPidList) {
            double personTotalValue = personTotalValueCalc;
            double shareSelfValue = 0;
            ClubMember playerClubMember;
            if (Config.isShare()) {
                playerClubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            } else {
                playerClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, pid);
            }
            if (Objects.nonNull(playerClubMember)) {
                //圈主是否计算过的标志
                boolean clubSubFlag = false;
                //圈主往下进行分成
                List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", playerClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
                List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
                ClubMember promotionUpLevel = null;
                //已经算分成的
                HashSet<Long> subUidList = new HashSet<>();
                //还没有算分成的
                HashSet<Long> notSubUidList = new HashSet<>();
                if (CollectionUtils.isNotEmpty(promotionList)) {
                    // 查询我的所有上线   从最上面那一层开始分
                    for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
                        //如果已经被计算过了 则当前这个玩家不再进行分成
                        if (subUidList.contains(queryUidOrPuidItem.getPuid())) {
                            continue;
                        }
                        if (personTotalValue == 0D) {
                            break;
                        }
                        ClubMember promotionMember;
                        if (Config.isShare()) {
                            promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getPuid());
                        } else {
                            promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getPuid());
                        }

                        if (Objects.nonNull(promotionMember)) {
                            //圈主开始往下分
                            if (clubCreate.getId() == promotionMember.getId() && !clubSubFlag) {
                                promotionUpLevel = promotionMember;
                                shareSelfValue = promotionSectionValue(playerClubMember, promotionMember, personTotalValue, false);
                                shareSelfValue = personTotalValue >= shareSelfValue ? shareSelfValue : personTotalValue;
                                personTotalValue = CommMath.subDouble(personTotalValue, shareSelfValue);
                                sumValue = CommMath.addDouble(sumValue, personTotalValueCalc);
                                //盟主的分成
                                this.saveUnionSportsPointProfitPromotion(unionCreate, unionId, shareSelfValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreatePid);
                                if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                                    unionCreateIncome = CommMath.addDouble(unionCreateIncome, shareSelfValue);
                                }
                                clubSubFlag = true;
                                //记录这个id已经分成过了
                                subUidList.add(promotionMember.getId());
                                if (notSubUidList.contains(promotionMember.getId())) {
                                    notSubUidList.remove(promotionMember.getId());
                                }
                                continue;
                            }
                            if (Objects.isNull(promotionUpLevel)) {
                                //如果找到圈主报错的话 这边来处理 看一下圈主是不是在里面
                                List<QueryUidOrPuidItem> clubCreateList = promotionList.stream().filter(k -> k.getPuid() == clubCreate.getId()).collect(Collectors.toList());
                                if (clubCreateList.size() > 0 && !clubSubFlag) {
                                    if (Config.isShare()) {
                                        promotionMember = ShareClubMemberMgr.getInstance().getClubMember(clubCreateList.get(0).getPuid());
                                    } else {
                                        promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubCreateList.get(0).getPuid());
                                    }
                                    shareSelfValue = promotionSectionValue(playerClubMember, promotionMember, personTotalValue, false);
                                    shareSelfValue = personTotalValue >= shareSelfValue ? shareSelfValue : personTotalValue;
                                    personTotalValue = CommMath.subDouble(personTotalValue, shareSelfValue);
                                    sumValue = CommMath.addDouble(sumValue, personTotalValueCalc);
                                    //盟主的分成
                                    this.saveUnionSportsPointProfitPromotion(unionCreate, unionId, shareSelfValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreatePid);
                                    if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                                        unionCreateIncome = CommMath.addDouble(unionCreateIncome, shareSelfValue);
                                    }
                                    clubSubFlag = true;
                                    //记录这个id已经分成过了
                                    subUidList.add(promotionMember.getId());
                                    if (notSubUidList.contains(promotionMember.getId())) {
                                        notSubUidList.remove(promotionMember.getId());
                                    }
                                    continue;
                                }
                            }
                            promotionMember = this.checkUpLevelClubMember(promotionMember, promotionUpLevel);
                            if (promotionMember.getId() != queryUidOrPuidItem.getPuid()) {
                                notSubUidList.add(queryUidOrPuidItem.getPuid());
                            }
                            // 推广员分成值
                            double promotionValue = promotionSectionValue(playerClubMember, promotionMember, personTotalValue, false);
                            promotionValue = personTotalValue >= promotionValue ? promotionValue : personTotalValue;
                            personTotalValue = CommMath.subDouble(personTotalValue, promotionValue);
                            //记录这个id已经分成过了
                            subUidList.add(promotionMember.getId());
                            if (notSubUidList.contains(promotionMember.getId())) {
                                notSubUidList.remove(promotionMember.getId());
                            }
                            //当前的代理战绩分成
                            this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                            //如果是创建者的话要扣掉对应的收入
                            if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                                unionCreateIncome = CommMath.addDouble(unionCreateIncome, promotionValue);
                            }
                            promotionUpLevel = promotionMember;
                        } else {
                            //添加日志 亲友圈成为null
                            CommLogD.error("RoomPromotionShare promotionMember is null  " + queryUidOrPuidItem.getPuid());
                        }

                    }
                    if (CollectionUtils.isNotEmpty(notSubUidList)) {
                        this.calcNotSubUidListSection(notSubUidList, playerClubMember, promotionUpLevel, personTotalValue, clubId, personTotalValueCalc);
                    }
                }
                // 如果当前玩家是推广员的话 则玩家也要参与到分成
                if (playerClubMember.isLevelPromotion() || playerClubMember.isClubCreate()) {
                    // 如果是圈主进来的话
                    if (playerClubMember.isClubCreate()) {
                        double promotionValue = promotionSectionValueCreate(playerClubMember);
                        promotionValue = personTotalValue >= promotionValue ? promotionValue : personTotalValue;
                        sumValue = CommMath.addDouble(sumValue, personTotalValueCalc);
                        personTotalValue = CommMath.subDouble(personTotalValue, promotionValue);
                        //盟主的分成
                        this.saveUnionSportsPointProfitPromotion(unionCreate, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreate.getClubMemberBO().getPlayerID());
                        if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                            unionCreateIncome = CommMath.addDouble(unionCreateIncome, promotionValue);
                        }
                        this.saveUnionSportsPointProfitPromotion(playerClubMember, unionId, personTotalValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), playerClubMember.getClubMemberBO().getPlayerID());
                    } else {
                        if (Objects.nonNull(promotionUpLevel) && personTotalValue > 0) {
                            // 推广员分成值
                            double promotionValue = promotionSectionValuePromotionSelf(playerClubMember);
                            promotionValue = personTotalValue >= promotionValue ? promotionValue : personTotalValue;
                            personTotalValue = CommMath.subDouble(personTotalValue, promotionValue);
                            //上级的分成
                            this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                            // 当前的代理战绩分成
                            this.saveUnionSportsPointProfitPromotion(playerClubMember, unionId, personTotalValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), playerClubMember.getClubMemberBO().getPlayerID());
                            //如果是创建者的话要扣掉对应的收入
                            if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                                unionCreateIncome = CommMath.addDouble(unionCreateIncome, promotionValue);
                            }
                        }
                    }
                } else if (Objects.isNull(promotionUpLevel)) {
                    //普通玩家区间的话 要经过盟主保留的
                    if (personTotalValue == 0D) {
                        break;
                    }
                    double promotionValue = promotionSectionValue(playerClubMember, clubCreate, personTotalValue, true);
                    promotionValue = personTotalValue >= promotionValue ? promotionValue : personTotalValue;
                    personTotalValue = CommMath.subDouble(personTotalValue, promotionValue);
                    sumValue = CommMath.addDouble(sumValue, personTotalValueCalc);
                    this.saveUnionSportsPointProfitPromotion(unionCreate, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreate.getClubMemberBO().getPlayerID());
                    // 当前的玩家是普通玩家 并且没经过上层 那么就由圈主收获盟主分下来的全部
                    if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                        unionCreateIncome = CommMath.addDouble(unionCreateIncome, promotionValue);
                    }
                    this.saveUnionSportsPointProfitPromotion(clubCreate, unionId, personTotalValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreate.getClubMemberBO().getPlayerID());
                }
            }
        }
        return sumValue;
    }

    /**
     * 没有计算的要计算
     *
     * @param notSubUidList
     * @param playerClubMember
     * @param promotionUpLevel
     */
    private void calcNotSubUidListSection(HashSet<Long> notSubUidList, ClubMember playerClubMember, ClubMember promotionUpLevel, double personTotalValue, long clubId, double personTotalValueCalc) {
        Union union;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }
        //已经算分成的
        HashSet<Long> subUidList = new HashSet<>();
        HashSet<Long> notSubUidListNew = new HashSet<>();
        if (CollectionUtils.isNotEmpty(notSubUidList)) {
            // 查询我的所有上线   从最上面那一层开始分
            for (Long queryUidOrPuidItem : notSubUidList) {
                //如果已经被计算过了 则当前这个玩家不再进行分成
                if (subUidList.contains(queryUidOrPuidItem)) {
                    continue;
                }
                if (personTotalValue == 0D) {
                    break;
                }
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem);
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem);
                }
                if (Objects.nonNull(promotionMember)) {
                    //圈主开始往下分
                    //检查当前的分成和上一个分成是不是直属的上下级 如果不是 重新找
                    promotionMember = this.checkUpLevelClubMember(promotionMember, promotionUpLevel);
                    if (promotionMember.getId() != queryUidOrPuidItem) {
                        notSubUidListNew.add(queryUidOrPuidItem);
                    }
                    // 推广员分成值
                    double promotionValue = promotionSectionValue(playerClubMember, promotionMember, personTotalValue, false);   //有一个代理选择不分给下一级 则没有必要继续进行
                    personTotalValue = CommMath.subDouble(personTotalValue, promotionValue);
                    //记录这个id已经分成过了
                    subUidList.add(promotionMember.getId());
                    if (notSubUidListNew.contains(promotionMember.getId())) {
                        notSubUidListNew.remove(promotionMember.getId());
                    }
                    //当前的代理战绩分成
                    this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                    //上一级扣除的分数
                    if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                        unionCreateIncome = CommMath.addDouble(unionCreateIncome, promotionValue);
                    }
//                    promotionUpLevel.getClubMemberBO().saveUnionSportsPointProfitPromotion(unionId, -promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Lose, roomId,dateTime);
                    promotionUpLevel = promotionMember;
                } else {
                    //添加日志 亲友圈成为null
                    CommLogD.error("RoomPromotionShare promotionMember is null  " + queryUidOrPuidItem);
                }
            }
            if (CollectionUtils.isNotEmpty(notSubUidListNew)) {
//                 暂时取消递归计算
                CommLogD.error("notSubUidList is forever:      " + "notSubUidList：" + notSubUidList.toString() + "notSubUidListNew   ：" + notSubUidListNew.toString() + "  roomId:" + roomId + "  clubID" + clubId + "   playerId:" + playerClubMember.getClubMemberBO().getPlayerID() + "personTotalValueCalc" + personTotalValueCalc + "  upplayerId   :" + promotionUpLevel.getClubMemberBO().getPlayerID());
            }
        }
    }

    /**
     * 获取分配给自己的值
     *
     * @param memberId
     * @param configId
     * @return
     */
    private Double getShareSectionItem(long memberId, long configId) {
        SharePromotionSection sharePromotionSection = SharePromotionSectionMgr.getInstance().getClubMemberPromotionSection(memberId);
        if (Objects.isNull(sharePromotionSection)) {
            return new Double(0);
        }
        PromotionShareSectionItem promotionShareSectionItems = sharePromotionSection.getPromotionShareSectionItems().stream().filter(k -> k.getUnionSectionId() == configId).findFirst().orElse(null);
        if (Objects.isNull(promotionShareSectionItems)) {
            return new Double(0);
        }
        return promotionShareSectionItems.getShareToSelfValue();
    }

    /**
     * 亲友圈成员推广员分成
     *
     * @param clubId            亲友圈Id
     * @param clubMemberPidList 亲友圈成员Pid列表
     * @param clubCreate        创建者
     * @param perConsume        一个人的贡献的竞技点
     */
    public void clubMemberPromotion(long clubId, List<Long> clubMemberPidList, ClubMember clubCreate, double perConsume, long unionCreatePid) {
        Union union;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }
        for (Long pid : clubMemberPidList) {
            //圈主是否计算过的标志
            boolean clubSubFlag = false;
            scorePercentCalc = 0D;
            ClubMember playerClubMember;
            if (Config.isShare()) {
                playerClubMember = ShareClubMemberMgr.getInstance().getClubMember(clubId, pid);
            } else {
                playerClubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMember(clubId, pid);
            }
            if (Objects.nonNull(playerClubMember)) {
                //圈主往下进行分成
                List<QueryUidOrPuidItem> queryUidOrPidItemList = ((ClubMemberRelationBOService) ContainerMgr.get().getComponent(ClubMemberRelationBOService.class)).findAllE(Restrictions.eq("uid", playerClubMember.getId()), QueryUidOrPuidItem.class, QueryUidOrPuidItem.getItemsNameId());
                List<QueryUidOrPuidItem> promotionList = queryUidOrPidItemList.stream().sorted(Comparator.comparing(QueryUidOrPuidItem::getId)).collect(Collectors.toList());
                ClubMember promotionUpLevel = null;
                // 分成类型
                int shareType = UnionDefine.UNION_SHARE_TYPE.PERCENT.ordinal();
                //已经算分成的
                HashSet<Long> subUidList = new HashSet<>();
                //还没有算分成的
                HashSet<Long> notSubUidList = new HashSet<>();
                if (CollectionUtils.isNotEmpty(promotionList)) {
                    // 查询我的所有上线   从最上面那一层开始分
                    for (QueryUidOrPuidItem queryUidOrPuidItem : promotionList) {
                        //如果已经被计算过了 则当前这个玩家不再进行分成
                        if (subUidList.contains(queryUidOrPuidItem.getPuid())) {
                            continue;
                        }
                        ClubMember promotionMember;
                        if (Config.isShare()) {
                            promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem.getPuid());
                        } else {
                            promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem.getPuid());
                        }
                        if (Objects.nonNull(promotionMember)) {
                            //圈主开始往下分
                            if (clubCreate.getId() == promotionMember.getId() && !clubSubFlag) {
                                promotionUpLevel = promotionMember;
                                if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                                    unionCreateIncome += perConsume;
                                }
                                clubSubFlag = true;
                                //圈主获得全部的 然后往下分
                                this.saveUnionSportsPointProfitPromotion(clubCreate, unionId, perConsume, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreatePid);
                                //记录这个id已经分成过了
                                subUidList.add(promotionMember.getId());
                                if (notSubUidList.contains(promotionMember.getId())) {
                                    notSubUidList.remove(promotionMember.getId());
                                }
                                if (promotionMember.getId() != queryUidOrPuidItem.getPuid()) {
                                    notSubUidList.add(queryUidOrPuidItem.getPuid());
                                }
                                continue;
                            }
                            //检查当前的分成和上一个分成是不是直属的上下级 如果不是 重新找
//                            if(Objects.isNull(promotionMember)){
//                                CommLogD.error("check null promotionMember");
//                                CommLogD.error(this.toString());
//                            }
                            if (Objects.isNull(promotionUpLevel)) {
//                                CommLogD.error("check null clubCreateList");
//                                CommLogD.error("this.toString() {}", this.toString());
//                                CommLogD.error("clubCreate={}, promotionMember={}", clubCreate.getId(), promotionMember.getId());
                                //如果找到圈主报错的话 这边来处理 看一下圈主是不是在里面
                                List<QueryUidOrPuidItem> clubCreateList = promotionList.stream().filter(k -> k.getPuid() == clubCreate.getId()).collect(Collectors.toList());
                                if (clubCreateList.size() > 0 && !clubSubFlag) {
                                    if (Config.isShare()) {
                                        promotionMember = ShareClubMemberMgr.getInstance().getClubMember(clubCreateList.get(0).getPuid());
                                    } else {
                                        promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(clubCreateList.get(0).getPuid());
                                    }
                                    promotionUpLevel = promotionMember;
                                    if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                                        unionCreateIncome += perConsume;
                                    }
                                    clubSubFlag = true;
                                    //圈主获得全部的 然后往下分
                                    this.saveUnionSportsPointProfitPromotion(clubCreate, unionId, perConsume, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), unionCreatePid);
                                    //记录这个id已经分成过了
                                    subUidList.add(promotionMember.getId());
                                    if (notSubUidList.contains(promotionMember.getId())) {
                                        notSubUidList.remove(promotionMember.getId());
                                    }
                                    if (promotionMember.getId() != queryUidOrPuidItem.getPuid()) {
                                        notSubUidList.add(queryUidOrPuidItem.getPuid());
                                    }
                                    continue;
                                }
                            }
                            promotionMember = this.checkUpLevelClubMember(promotionMember, promotionUpLevel);
                            //找不到对应的上级直接退出并记录
                            if (Objects.isNull(promotionMember)) {
                                //添加日志 亲友圈成为null
                                CommLogD.error("RoomPromotionShare promotionMember is null : uid " + queryUidOrPuidItem.getPuid() + " uplevelID:" + promotionUpLevel.getId() + "  roomId:" + roomId + "  clubID" + clubId + "   playerInRoomID:" + pid);
                                break;
                            }
                            if (promotionMember.getId() != queryUidOrPuidItem.getPuid()) {
                                notSubUidList.add(queryUidOrPuidItem.getPuid());
                            }
                            // 不是固定值,重新设置值
                            shareType = shareType != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() ? promotionMember.getClubMemberBO().getShareType() : shareType;
                            // 推广员分成值
                            double promotionValue = this.promotionValue(promotionMember.getClubMemberBO().getPlayerID(), promotionMember.getClubID(), shareType, perConsume, promotionMember.getClubMemberBO().getShareValue(), promotionMember.getClubMemberBO().getShareFixedValue());
                            //有一个代理选择不分给下一级 则没有必要继续进行
                            if (promotionValue == 0D) {
                                // TODO 2020/9/24 日志太多先进行注释，需要时再打开
                                // CommLog.info("clubMemberPromotion RoomId:{},clubId:{},Pid:{},perConsume:{},shareType:{},ShareValue:{},ShareFixedValue:{}", getRoomId(),clubId,promotionMember.getClubMemberBO().getPlayerID(),perConsume,shareType,promotionMember.getClubMemberBO().getShareValue(), promotionMember.getClubMemberBO().getShareFixedValue());
                                break;
                            }
                            //记录这个id已经分成过了
                            subUidList.add(promotionMember.getId());
                            if (notSubUidList.contains(promotionMember.getId())) {
                                notSubUidList.remove(promotionMember.getId());
                            }
                            //当前的代理战绩分成
                            this.saveUnionSportsPointProfitPromotion(promotionMember, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                            //如果是创建者的话要扣掉对应的收入
                            if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                                unionCreateIncome -= promotionValue;
                            }
                            //上一级扣除的分数
                            this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, -promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Lose, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionMember.getClubMemberBO().getPlayerID());
                            promotionUpLevel = promotionMember;
                        } else {
                            //添加日志 亲友圈成为null
                            CommLogD.error("RoomPromotionShare promotionMember is null  " + queryUidOrPuidItem.getPuid());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(notSubUidList)) {
                        this.calcNotSubUidList(notSubUidList, playerClubMember, promotionUpLevel, shareType, clubId, perConsume);
                    }
                }
                // 不是固定值,重新设置值
                shareType = shareType != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() ? playerClubMember.getClubMemberBO().getShareType() : shareType;
                // 如果当前玩家是推广员的话 则玩家也要参与到分成
                if (playerClubMember.isLevelPromotion() || playerClubMember.isClubCreate()) {
                    // 如果是圈主进来的话
                    if (playerClubMember.isClubCreate()) {
                        if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                            unionCreateIncome += perConsume;
                        }
                        this.saveUnionSportsPointProfitPromotion(playerClubMember, unionId, perConsume, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), playerClubMember.getClubMemberBO().getPlayerID());
                    } else {
                        if (Objects.nonNull(promotionUpLevel)) {
                            if (promotionUpLevel.getId() != playerClubMember.getClubMemberBO().getUpLevelId()) {
                                if (Config.isShare()) {
                                    promotionUpLevel = ShareClubMemberMgr.getInstance().getClubMember(playerClubMember.getClubMemberBO().getUpLevelId());
                                } else {
                                    promotionUpLevel = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(playerClubMember.getClubMemberBO().getUpLevelId());
                                }
                            }
                            // 推广员分成值
                            double promotionValue = this.promotionValue(playerClubMember.getClubMemberBO().getPlayerID(), playerClubMember.getClubID(), shareType, perConsume, playerClubMember.getClubMemberBO().getShareValue(), playerClubMember.getClubMemberBO().getShareFixedValue());
                            // 当前的代理战绩分成
                            this.saveUnionSportsPointProfitPromotion(playerClubMember, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                            //如果是创建者的话要扣掉对应的收入
                            if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                                unionCreateIncome -= promotionValue;
                            }
                            if (Objects.nonNull(promotionUpLevel)) {
                                this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, -promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Lose, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), playerClubMember.getClubMemberBO().getPlayerID());
                            }
//                            else {
//                                CommLogD.error("check null promotionUpLevel");
//                                CommLogD.error("this.toString() {}", this.toString());
//                                CommLogD.error("clubCreate={}, promotionMember={}", clubCreate.getId(), playerClubMember.getId());
//                            }
                        }
                    }
                } else if (Objects.isNull(promotionUpLevel)) {
                    // 当前的玩家是普通玩家 并且没经过上层 那么就由圈主收获盟主分下来的全部
                    if (union.getUnionBO().getClubId() == clubCreate.getClubID()) {
                        unionCreateIncome += perConsume;
                    }
                    this.saveUnionSportsPointProfitPromotion(clubCreate, unionId, perConsume, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), clubCreate.getClubMemberBO().getPlayerID());
                }
            } else {
                // 添加日志 判断是否亲友圈 圈住id 为0
                CommLogD.error("RoomPromotionShare clubCreateId is null  two" + clubId);
            }

        }
    }

    /**
     * 没有计算的要计算
     *
     * @param notSubUidList
     * @param playerClubMember
     * @param promotionUpLevel
     * @param shareType
     */
    private void calcNotSubUidList(HashSet<Long> notSubUidList, ClubMember playerClubMember, ClubMember promotionUpLevel, int shareType, long clubId, double perConsume) {
        Union union;
        if (Config.isShare()) {
            union = ShareUnionListMgr.getInstance().getUnion(this.getUnionId());
        } else {
            union = UnionMgr.getInstance().getUnionListMgr().findUnion(this.getUnionId());
        }
        //已经算分成的
        HashSet<Long> subUidList = new HashSet<>();
        HashSet<Long> notSubUidListNew = new HashSet<>();
        if (CollectionUtils.isNotEmpty(notSubUidList)) {
            // 查询我的所有上线   从最上面那一层开始分
            for (Long queryUidOrPuidItem : notSubUidList) {
                //如果已经被计算过了 则当前这个玩家不再进行分成
                if (subUidList.contains(queryUidOrPuidItem)) {
                    continue;
                }
                ClubMember promotionMember;
                if (Config.isShare()) {
                    promotionMember = ShareClubMemberMgr.getInstance().getClubMember(queryUidOrPuidItem);
                } else {
                    promotionMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(queryUidOrPuidItem);
                }
                if (Objects.nonNull(promotionMember)) {
                    //圈主开始往下分
                    //检查当前的分成和上一个分成是不是直属的上下级 如果不是 重新找
                    promotionMember = this.checkUpLevelClubMember(promotionMember, promotionUpLevel);
//                    //找不到对应的上级
//                    if (Objects.isNull(promotionMember)) {
//                        //添加日志 亲友圈成为null
//                        CommLogD.error("RoomPromotionShare promotionMember is null : uid " + queryUidOrPuidItem + " uplevelID:" + promotionUpLevel.getId() + "  roomId:" + roomId + "  clubID" + clubId + "   playerInRoomID inside:");
//                    }
                    if (promotionMember.getId() != queryUidOrPuidItem) {
                        notSubUidListNew.add(queryUidOrPuidItem);
                    }
                    // 不是固定值,重新设置值
                    shareType = shareType != UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() ? promotionMember.getClubMemberBO().getShareType() : shareType;
                    // 推广员分成值
                    double promotionValue = this.promotionValue(promotionMember.getClubMemberBO().getPlayerID(), promotionMember.getClubID(), shareType, perConsume, promotionMember.getClubMemberBO().getShareValue(), promotionMember.getClubMemberBO().getShareFixedValue());

                    //有一个代理选择不分给下一级 则没有必要继续进行
                    if (promotionValue == 0D) {
                        // TODO 2020/9/24 日志太多先进行注释，需要时再打开
                        // CommLog.info("clubMemberPromotion RoomId:{},clubId:{},Pid:{},perConsume:{},shareType:{},ShareValue:{},ShareFixedValue:{}", getRoomId(),clubId,promotionMember.getClubMemberBO().getPlayerID(),perConsume,shareType,promotionMember.getClubMemberBO().getShareValue(), promotionMember.getClubMemberBO().getShareFixedValue());
                        break;
                    }
                    //记录这个id已经分成过了
                    subUidList.add(promotionMember.getId());
                    if (notSubUidListNew.contains(promotionMember.getId())) {
                        notSubUidListNew.remove(promotionMember.getId());
                    }
                    //当前的代理战绩分成
                    this.saveUnionSportsPointProfitPromotion(promotionMember, unionId, promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Gain, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionUpLevel.getClubMemberBO().getPlayerID());
                    //上一级扣除的分数
                    if (promotionUpLevel.isClubCreate() && union.getUnionBO().getClubId() == promotionUpLevel.getClubID()) {
                        unionCreateIncome -= promotionValue;
                    }
                    this.saveUnionSportsPointProfitPromotion(promotionUpLevel, unionId, -promotionValue, sourceType, roomName, roomKey, Id, classificationRegionId, ConstEnum.ResOpType.Lose, roomId, dateTime, playerClubMember.getClubMemberBO().getPlayerID(), promotionMember.getClubMemberBO().getPlayerID());
                    promotionUpLevel = promotionMember;
                } else {
                    //添加日志 亲友圈成为null
                    CommLogD.error("RoomPromotionShare promotionMember is null  " + queryUidOrPuidItem);
                }
            }
            if (CollectionUtils.isNotEmpty(notSubUidListNew)) {
//                 暂时取消递归计算
                CommLogD.error("notSubUidList is forever:      " + "notSubUidList：" + notSubUidList.toString() + "notSubUidListNew   ：" + notSubUidListNew.toString() + "  roomId:" + roomId + "  clubID" + clubId + "   playerId:" + playerClubMember.getClubMemberBO().getPlayerID() + "perCOnsume" + perConsume + "  upplayerId   :" + promotionUpLevel.getClubMemberBO().getPlayerID());
//                this.calcNotSubUidList(notSubUidList, playerClubMember, promotionUpLevel, shareType, clubId, perConsume);
            }
        }
    }

    /**
     * 获取某个玩家的直属下级
     *
     * @param promotionMember
     * @param promotionUpLevel
     * @return
     */
    private ClubMember checkUpLevelClubMember(ClubMember promotionMember, ClubMember promotionUpLevel) {
        if (Objects.isNull(promotionMember) || Objects.isNull(promotionUpLevel)) {
            return null;
        }
        //如果是直属下级 那就是了
        if (promotionMember.getClubMemberBO().getUpLevelId() == promotionUpLevel.getId()) {
            return promotionMember;
        }
        //不是的话从当前玩家的的上级开始找 找到是为止  如果找不到 记录报错信息
        ClubMember clubMember;
        if (Config.isShare()) {
            clubMember = ShareClubMemberMgr.getInstance().getClubMember(promotionMember.getClubMemberBO().getUpLevelId());
        } else {
            clubMember = ClubMgr.getInstance().getClubMemberMgr().getClubMemberMap().get(promotionMember.getClubMemberBO().getUpLevelId());
        }
        if (Objects.isNull(clubMember)) {
            return null;
        }
        return checkUpLevelClubMember(clubMember, promotionUpLevel);
    }

    /**
     * 推广员区间分成值
     *
     * @return
     */
    private double promotionSectionValue(ClubMember playerClubMember, ClubMember shareClubMember, double personTotalValue, boolean noramal) {
        Double shareToSelfValue = this.getShareSectionItem(shareClubMember.getId(), sectionId);
        if (noramal) {
            return CommMath.div(shareToSelfValue, pidMap.size());
        }
        //如果是推广员
        if (playerClubMember.isLevelPromotion()) {
            if (playerClubMember.getClubMemberBO().getId() == shareClubMember.getClubMemberBO().getId()) {
                return personTotalValue;
            } else {
                return CommMath.div(shareToSelfValue, pidMap.size());
            }
        }
        if (playerClubMember.getClubMemberBO().getUpLevelId() == shareClubMember.getClubMemberBO().getId() ||
                (playerClubMember.getClubMemberBO().getUpLevelId() == 0 && shareClubMember.isClubCreate() && (shareClubMember.getClubID() == playerClubMember.getClubID()))) {
            return personTotalValue;
        } else {
            return CommMath.div(shareToSelfValue, pidMap.size());
        }
    }

    /**
     * 推广员区间分成值
     *
     * @return
     */
    private double promotionSectionValuePromotionSelf(ClubMember playerClubMember) {
        Double shareToSelfValue = this.getShareSectionItem(playerClubMember.getId(), sectionId);
        return CommMath.div(shareToSelfValue, pidMap.size());
    }

    /**
     * 推广员区间分成值
     *
     * @return
     */
    private double promotionSectionValueCreate(ClubMember playerClubMember) {
        Double shareToSelfValue = this.getShareSectionItem(playerClubMember.getId(), sectionId);
        return CommMath.div(shareToSelfValue, pidMap.size());
    }

    /**
     * 推广员分成值
     *
     * @param pid             玩家Pid
     * @param clubId          亲友圈Id
     * @param shareType       分成类型
     * @param perConsume      一个人的贡献的竞技点
     * @param shareValue      百分比
     * @param shareFixedValue 固定值
     * @return
     */
    private double promotionValue(long pid, long clubId, int shareType, double perConsume, double shareValue, double shareFixedValue) {
        if (UnionDefine.UNION_SHARE_TYPE.FIXED.ordinal() == shareType) {
            // 固定值
            return ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class).findScorePercen(pid, getUnionId(), clubId, getConfigId(), shareType, shareFixedValue);
        } else {
            // 存在数据库的是乘以一百后的  所以计算的时候除以100
            double scorePercent = CommMath.divThreePoint(ContainerMgr.get().getComponent(PromotionLevelRoomConfigScorePercentBOService.class).findScorePercen(pid, getUnionId(), clubId, getConfigId(), shareType, shareValue), 100);
            //如果当前没值的话 进行赋值
            if (scorePercentCalc == 0D) {
                scorePercentCalc = scorePercent;
            } else {
                scorePercent = scorePercent > scorePercentCalc ? scorePercentCalc : scorePercent;
                scorePercentCalc = scorePercent;
            }
            // 推广员自己的分成
            return CommMath.mul(scorePercent, perConsume);
        }
    }


    /**
     * 盟主获得
     */
    public void saveUnionSportsPointProfitUnion(ClubMember clubMember, long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ConstEnum.ResOpType resOpType, long roomId, Map<Long, Double> clubOwnerProfit) {
        if (this.caseSportsFlag) {
            //如果是扣分的话要考虑 保险箱分数不低于0  不足的 从竞技点去扣   4  -5
            if (value < 0) {
                double casePoint = clubMember.getClubMemberBO().getCaseSportsPoint();
                double subValue = CommMath.addDouble(casePoint, value);
                if (subValue < 0) {
                    clubMember.getClubMemberBO().saveUnionSportsPointProfitToCasePoint(unionId, -casePoint, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, true, clubOwnerProfit);
                    clubMember.getClubMemberBO().saveUnionSportsPointProfit(unionId, subValue, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, false, clubOwnerProfit);
                    return;
                }
            }
            clubMember.getClubMemberBO().saveUnionSportsPointProfitToCasePoint(unionId, value, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, true, clubOwnerProfit);
        } else {
            clubMember.getClubMemberBO().saveUnionSportsPointProfit(unionId, value, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, true, clubOwnerProfit);
        }
    }

    /**
     * 推广员获得
     */
    public void saveUnionSportsPointProfitPromotion(ClubMember clubMember, long unionId, double value, int sourceType, String roomName, int roomKey, int gameId, int classificationRegionId, ConstEnum.ResOpType resOpType, long roomId, String dateTime, long execPid, long reasonPid) {
        if (this.caseSportsFlag) {
            //如果是扣分的话要考虑 保险箱分数不低于0  不足的 从竞技点去扣   4  -5
            if (value < 0) {
                double casePoint = clubMember.getClubMemberBO().getCaseSportsPoint();
                double subValue = CommMath.addDouble(casePoint, value);
                if (subValue < 0) {
                    clubMember.getClubMemberBO().saveUnionSportsPointProfitPromotionToCasePoint(unionId, -casePoint, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, dateTime, execPid, reasonPid, this.getDateTimeZhongZhi());
                    clubMember.getClubMemberBO().saveUnionSportsPointProfitPromotion(unionId, subValue, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, dateTime, execPid, reasonPid, this.getDateTimeZhongZhi());
                    return;
                }
            }
            clubMember.getClubMemberBO().saveUnionSportsPointProfitPromotionToCasePoint(unionId, value, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, dateTime, execPid, reasonPid, this.getDateTimeZhongZhi());
        } else {
            clubMember.getClubMemberBO().saveUnionSportsPointProfitPromotion(unionId, value, sourceType, roomName, roomKey, gameId, classificationRegionId, resOpType, roomId, dateTime, execPid, reasonPid, this.getDateTimeZhongZhi());
        }
    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PROMOTION_SHARE.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PROMOTION_SHARE.bufferSize();
    }

    @Override
    public String toString() {
        return "RoomPromotionShare{" +
                "roomSportsPointConsume=" + roomSportsPointConsume +
                ", roomSportsPointConsumePrizePool=" + roomSportsPointConsumePrizePool +
                ", unionId=" + unionId +
                ", pidMap=" + pidMap +
                ", roomName='" + roomName + '\'' +
                ", roomKey=" + roomKey +
                ", configId=" + configId +
                ", roomId=" + roomId +
                ", clubMap=" + clubMap +
                ", Id=" + Id +
                ", classificationRegionId=" + classificationRegionId +
                ", prizePool=" + prizePool +
                ", sourceType=" + sourceType +
                ", dateTime='" + dateTime + '\'' +
                ", setCount=" + setCount +
                ", dataJsonCfg='" + dataJsonCfg + '\'' +
                ", consumeValue=" + consumeValue +
                ", gameId=" + gameId +
                ", unionCreateIncome=" + unionCreateIncome +
                '}';
    }
}
