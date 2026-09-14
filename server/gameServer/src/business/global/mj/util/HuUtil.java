package business.global.mj.util;

import business.global.mj.MJCardInit;
import cenum.mj.MJSpecialEnum;
import com.ddm.server.common.utils.CommString;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.common.utils.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HuUtil {
    public static final List<Integer> CheckTypes;

    static {
        List<Integer> checkTypes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 4; j++) {
                int type = j * 10 + i;
                if (type > 47) {
                    continue;
                }
                checkTypes.add(type);
            }
        }
        CheckTypes = Collections.unmodifiableList(checkTypes);
    }


    public static final List<Integer> CheckHuaTypes;

    static {
        List<Integer> checkHuaTypes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 4; j++) {
                int type = j * 10 + i;
                if (type > 47) {
                    continue;
                }
                checkHuaTypes.add(type);
            }
        }
        for (int i = 1; i <= 8; i++) {
            checkHuaTypes.add(50 + i);
        }
        CheckHuaTypes = Collections.unmodifiableList(checkHuaTypes);
    }

    private volatile static HuUtil singleton;

    private HuUtil() {
    }

    public static HuUtil getInstance() {
        if (singleton == null) {
            synchronized (HuUtil.class) {
                if (singleton == null) {
                    singleton = new HuUtil();
                }
            }
        }
        return singleton;
    }

    public boolean checkBaiBanHuCard(List<Integer> allCardInts, int totalJin, List<Integer> Jins) {
        if (Jins.size() <= 0) {
            return false;
        }
        List<Integer> danBaiBanList = checkDanBaiBan(allCardInts, Jins);
        if (null == danBaiBanList) {
            danBaiBanList = allCardInts;
        }
        return check3nP2(danBaiBanList, totalJin);
    }


    /**
     * 白板替金
     *
     * @param allCardInts
     * @param Jins
     * @param totalJin
     * @param cardType
     * @return
     */
    public boolean checkBTJHu(List<Integer> allCards, int totalJin, List<Integer> Jins) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return checkBaiBanHuCard(allCards, totalJin, Jins);
    }


    /**
     * 胡
     *
     * @param allCardInts
     * @param Jins
     * @param totalJin
     * @param cardType
     * @return
     */
    public boolean checkHu(MJCardInit mCardInit) {
        List<Integer> allCards = mCardInit.getAllCardInts();
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return check3nP2(allCards, mCardInit.sizeJin());
    }


    /**
     * 胡
     *
     * @param allCardInts
     * @param Jins
     * @param totalJin
     * @param cardType
     * @return
     */
    public boolean checkHu(List<Integer> allCards, int totalJin) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return check3nP2(allCards, totalJin);
    }

    /**
     * 获取胡牌类型
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> findHuTypeList(List<Integer> allCards, int totalJin) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        List<String> resultList = checkPossibleGroup(allCards, totalJin);
        return resultList;
    }

    public boolean check3nP2(List<Integer> allCards, int totalJin) {
        List<String> resultList = checkPossibleGroup(allCards, totalJin);
        return resultList.size() > 0;
    }

    private final String allJin(int totalJin) {
        if (totalJin >= 2) {
            String end = CommString.join(",", Arrays.asList(0, 0));
            int leftJin = totalJin - 2;
            if (leftJin % 3 == 0) {
                for (int i = 1; i <= totalJin / 3; i++) {
                    end += "  :  " + "0,0,0";
                }
            }
            return end;
        }
        return null;
    }

    /**
     * 检查可能性组合
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> checkPossibleGroup(List<Integer> allCards, int totalJin) {
        List<String> resultList = new ArrayList<>();
        if (allCards.size() <= 0 && totalJin > 0) {
            String jinStr = allJin(totalJin);
            if (StringUtils.isNotEmpty(jinStr)) {
                resultList.add(jinStr);
            }
            return resultList;
        }


        List<Integer> typeList = new ArrayList<>(); // 计算牌的品类
        int lastType = -1;
        for (int type : allCards) {
            if (type != lastType) {
                typeList.add(type);
                lastType = type;
            }
        }

        // 遍历每种牌
        for (Integer type : typeList) {
            List<Integer> dui = new ArrayList<>();
            List<Integer> leftCards = new ArrayList<>(allCards);
            leftCards.remove(type);
            dui.add(type);
            int leftJin = totalJin;
            if (leftCards.contains(type)) {
                leftCards.remove(type);
                dui.add(type);
            } else {
                if (leftJin == 0) {
                    continue;
                }
                leftJin -= 1;
                dui.add(0);
            }

            List<List<Integer>> matched = new ArrayList<>();
            //
            matchThird(resultList, 0, dui, matched, leftJin, leftCards);
        }
        return resultList;
    }

    public void matchThird(List<String> resultList, int tabCnt,
                           List<Integer> dui, List<List<Integer>> matched, int leftJin,
                           List<Integer> leftCards) {
        if (leftCards.size() == 0 && leftJin % 3 == 0) {
            String end = CommString.join(",", dui);
            for (List<Integer> match : matched) {
                end += "  :  " + CommString.join(",", match);
            }
            for (int i = 1; i <= leftJin / 3; i++) {
                end += "  :  " + "0,0,0";
            }
            if (!resultList.contains(end)) {
                resultList.add(end);
            }
            return;
        }

        if (leftCards.size() == 0) {
            return;
        }
        // 取一个值
        Integer type = leftCards.get(0);
        leftCards.remove(type);

        // 尝试匹配 3type
        List<Integer> newMatched = new ArrayList<>();
        List<Integer> newLeftCard = matchKe(new ArrayList<>(leftCards), type,
                leftJin, newMatched);
        if (null != newLeftCard) {
            List<List<Integer>> newMatchList = new ArrayList<>(matched);
            newMatchList.add(newMatched);
            int newLeftJin = leftJin - calcNum(newMatched, 0);
            matchThird(resultList, tabCnt + 1, dui, newMatchList, newLeftJin,
                    newLeftCard);
        }
        // 尝试匹配 shun1
        List<Integer> newMatched1 = new ArrayList<>();
        List<Integer> newLeftCard1 = matchShunLeft(new ArrayList<>(leftCards),
                type, leftJin, newMatched1);
        if (null != newLeftCard1) {
            List<List<Integer>> newMatchList1 = new ArrayList<>(matched);
            newMatchList1.add(newMatched1);
            int newLeftJin = leftJin - calcNum(newMatched1, 0);
            matchThird(resultList, tabCnt + 1, dui, newMatchList1, newLeftJin,
                    newLeftCard1);
        }
        // 尝试匹配 shun2
        List<Integer> newMatched2 = new ArrayList<>();
        List<Integer> newLeftCard2 = matchShunMid(new ArrayList<>(leftCards),
                type, leftJin, newMatched2);
        if (null != newLeftCard2) {
            List<List<Integer>> newMatchList2 = new ArrayList<>(matched);
            newMatchList2.add(newMatched2);
            int newLeftJin = leftJin - calcNum(newMatched2, 0);
            matchThird(resultList, tabCnt + 1, dui, newMatchList2, newLeftJin,
                    newLeftCard2);
        }
        // 尝试匹配 shun3
        List<Integer> newMatched3 = new ArrayList<>();
        List<Integer> newLeftCard3 = matchShunRight(new ArrayList<>(leftCards),
                type, leftJin, newMatched3);
        if (null != newLeftCard3) {
            List<List<Integer>> newMatchList3 = new ArrayList<>(matched);
            newMatchList3.add(newMatched3);
            int newLeftJin = leftJin - calcNum(newMatched3, 0);
            matchThird(resultList, tabCnt + 1, dui, newMatchList3, newLeftJin,
                    newLeftCard3);
        }
    }

    /**
     * 获取胡牌类型
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> findHuTypeList1(List<Integer> allCards, int totalJin) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        List<String> resultList = new ArrayList<>();
        if (allCards.size() <= 0 && totalJin > 0) {
            String jinStr = allJin(totalJin);
            if (StringUtils.isNotEmpty(jinStr)) {
                resultList.add(jinStr);
            }
            return resultList;
        }


        List<Integer> typeList = new ArrayList<>(); // 计算牌的品类
        int lastType = -1;
        for (int type : allCards) {
            if (type != lastType) {
                typeList.add(type);
                lastType = type;
            }
        }

        // 遍历每种牌
        for (Integer type : typeList) {
            List<Integer> dui = new ArrayList<>();
            List<Integer> leftCards = new ArrayList<>(allCards);
            leftCards.remove(type);
            dui.add(type);
            int leftJin = totalJin;
            if (leftCards.contains(type)) {
                leftCards.remove(type);
                dui.add(type);
            } else {
                if (leftJin == 0) {
                    continue;
                }
                leftJin -= 1;
                dui.add(0);
            }

            List<List<Integer>> matched = new ArrayList<>();
            //
            matchThird(resultList, 0, dui, matched, leftJin, leftCards);
        }
        //金做将牌
        if (totalJin > 0) {
            // 遍历每种牌
            int limit = totalJin >= 2 ? 2 : 1;
            for (int p = 1; p <= limit; p++) {
                List<Integer> dui = new ArrayList<>(Collections.nCopies(p, 0));
                int leftJin = totalJin - p;
                if (dui.size() < 2) {
                    for (Integer type : typeList) {
                        List<Integer> leftCards = new ArrayList<>(allCards);
                        leftCards.remove(type);
                        dui.add(type);
                        List<List<Integer>> matched = new ArrayList<>();
                        matchThird(resultList, 0, dui, matched, leftJin, leftCards);
                        dui.remove(type);
                    }
                } else {
                    List<Integer> leftCards = new ArrayList<>(allCards);
                    List<List<Integer>> matched = new ArrayList<>();
                    matchThird(resultList, 0, dui, matched, leftJin, leftCards);
                }
            }
        }
        return resultList;
    }


    /**
     * 获取胡牌类型
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> findHuTypeList2(List<Integer> allCards, int totalJin, Function<Integer, Boolean> function) {
        Collections.sort(allCards, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        List<String> resultList = new ArrayList<>();
        if (allCards.size() <= 0 && totalJin > 0) {
            String jinStr = allJin(totalJin);
            if (StringUtils.isNotEmpty(jinStr)) {
                resultList.add(jinStr);
            }
            return resultList;
        }


        List<Integer> typeList = new ArrayList<>(); // 计算牌的品类
        int lastType = -1;
        for (int type : allCards) {
            if (type != lastType && function.apply(type)) {
                typeList.add(type);
                lastType = type;
            }
        }

        // 遍历每种牌
        for (Integer type : typeList) {
            List<Integer> dui = new ArrayList<>();
            List<Integer> leftCards = new ArrayList<>(allCards);
            leftCards.remove(type);
            dui.add(type);
            int leftJin = totalJin;
            if (leftCards.contains(type)) {
                leftCards.remove(type);
                dui.add(type);
            } else {
                if (leftJin == 0) {
                    continue;
                }
                leftJin -= 1;
                dui.add(0);
            }

            List<List<Integer>> matched = new ArrayList<>();
            //
            matchThird(resultList, 0, dui, matched, leftJin, leftCards);
        }
        //金做将牌
        if (totalJin > 0) {
            // 遍历每种牌
            int limit = totalJin >= 2 ? 2 : 1;
            for (int p = 1; p <= limit; p++) {
                List<Integer> dui = new ArrayList<>(Collections.nCopies(p, 0));
                int leftJin = totalJin - p;
                if (dui.size() < 2) {
                    for (Integer type : typeList) {
                        List<Integer> leftCards = new ArrayList<>(allCards);
                        leftCards.remove(type);
                        dui.add(type);
                        List<List<Integer>> matched = new ArrayList<>();
                        matchThird(resultList, 0, dui, matched, leftJin, leftCards);
                        dui.remove(type);
                    }
                } else {
                    List<Integer> leftCards = new ArrayList<>(allCards);
                    List<List<Integer>> matched = new ArrayList<>();
                    matchThird(resultList, 0, dui, matched, leftJin, leftCards);
                }
            }
        }
        return resultList;
    }

    public List<Integer> matchKe(List<Integer> srcCards, Integer type, int jin,
                                 List<Integer> newMatched) {
        newMatched.add(type);
        if (srcCards.contains(type)) {
            srcCards.remove(type);
            newMatched.add(type);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        if (srcCards.contains(type)) {
            srcCards.remove(type);
            newMatched.add(type);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchShunLeft(List<Integer> srcCards, Integer type,
                                       int jin, List<Integer> newMatched) {
        if (type > 40) {
            return null;
        }

        if (type % 10 + 1 > 9 || type % 10 + 2 > 9) {
            return null;
        }

        newMatched.add(type);
        Integer right1 = type + 1;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        Integer right2 = type + 2;
        if (srcCards.contains(right2)) {
            srcCards.remove(right2);
            newMatched.add(right2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchShunMid(List<Integer> srcCards, Integer type,
                                      int jin, List<Integer> newMatched) {
        if (type > 40) {
            return null;
        }
        if (type % 10 + 1 > 9 || type % 10 - 1 < 1) {
            return null;
        }

        newMatched.add(type);
        Integer right1 = type + 1;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchShunRight(List<Integer> srcCards, Integer type,
                                        int jin, List<Integer> newMatched) {
        if (type > 40) {
            return null;
        }
        if (type % 10 - 1 < 1 || type % 10 - 2 < 1) {
            return null;
        }

        newMatched.add(type);
        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        Integer left2 = type - 2;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        return jin >= 0 ? srcCards : null;
    }

    public int calcNum(List<Integer> total, int match) {
        int ret = 0;

        for (int i : total) {
            if (i == match) {
                ret += 1;
            }
        }

        return ret;
    }


    /**
     * 检查白板
     *
     * @return
     */
    public List<Integer> checkDanBaiBan(List<Integer> allCardInts,
                                        List<Integer> Jins) {
        if (null == Jins || Jins.size() <= 0) {
            return null;
        }
        // 判断金的大小，是否属于1\2\3中
        List<Integer> baiBans = new ArrayList<Integer>();
        List<Integer> baList = new ArrayList<Integer>();
        baList.addAll(allCardInts);
        // 如果金大于40就没必要进行接下去的判断了。
        if (Jins.get(0) > 40) {
            return null;
        }
        // 检查是否有白板
        for (int i = 0; i < baList.size(); i++) {
            if (baList.get(i) == MJSpecialEnum.BAIBAN.value()) {
                baiBans.add(baList.get(i));
            }
        }
        // 检查白板的数量
        if (baiBans.size() <= 0) {
            return null;
        }
        // 去除所有的白板
        baList.removeAll(baiBans);
        // 将数组中的白板去掉，换成金的数字
        for (int i = 0; i < baiBans.size(); i++) {
            baList.add(Jins.get(0));
        }
        // 对结果进行排序
        Collections.sort(baList, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                // 从小到大
                return o1 - o2;
            }
        });
        return baList;
    }

    /**
     * 检查白板
     *
     * @return
     */
    public HashMap<Integer, List<Integer>> checkBTJAll(List<Integer> allCardInts,
                                                       List<Integer> Jins) {
        if (Jins.size() <= 0) {
            return null;
        }
        // 判断金的大小，是否属于1\2\3中
        List<Integer> baiBans = new ArrayList<Integer>();
        List<Integer> baList = new ArrayList<Integer>();
        baList.addAll(allCardInts);
        // 如果金大于40就没必要进行接下去的判断了。
        if (Jins.get(0) > 40) {
            return null;
        }
        // 检查是否有白板
        for (int i = 0; i < baList.size(); i++) {
            if (baList.get(i) == MJSpecialEnum.BAIBAN.value()) {
                baiBans.add(baList.get(i));
            }
        }
        // 检查白板的数量
        if (baiBans.size() <= 0) {
            return null;
        }
        // 去除所有的白板
        baList.removeAll(baiBans);
        // 检查出白板替金的所有情况

        HashMap<Integer, List<Integer>> BTJallListMap = new HashMap<Integer, List<Integer>>();
        HashMap<Integer, List<Integer>> BTJallMap = baiBanTiJin(baiBans, Jins.get(0));
        List<Integer> BTJList = null;
        List<Integer> BTJs = null;
        for (int i = 0, size = BTJallMap.size(); i < size; i++) {
            BTJList = new ArrayList<Integer>();
            BTJs = BTJallMap.get(i);
            BTJList.addAll(baList);
            BTJList.addAll(BTJs);
            BTJallListMap.put(i, BTJList);
        }
        return BTJallListMap;
    }

    /**
     * 判断所有白板替金的情况。
     * 将白板替金的所以情况都列出来，便于遍历。
     *
     * @param baiBans
     * @return
     */
    private HashMap<Integer, List<Integer>> baiBanTiJin(List<Integer> baiBans, int JinCard) {
        HashMap<Integer, List<Integer>> mHashMap = new HashMap<Integer, List<Integer>>();
        List<Integer> aList = new ArrayList<Integer>();
        if (baiBans.size() == 1) {
            aList.add(JinCard);
            mHashMap.put(0, aList);
            aList = new ArrayList<Integer>();
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(1, aList);
        } else if (baiBans.size() == 2) {
            aList.add(JinCard);
            aList.add(JinCard);
            mHashMap.put(0, aList);
            aList = new ArrayList<Integer>();
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(1, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(2, aList);
        } else if (baiBans.size() == 3) {
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(JinCard);
            mHashMap.put(0, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(1, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(2, aList);
            aList = new ArrayList<Integer>();
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(3, aList);
        } else if (baiBans.size() == 4) {
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(JinCard);
            mHashMap.put(0, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(1, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(2, aList);
            aList = new ArrayList<Integer>();
            aList.add(JinCard);
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(3, aList);
            aList = new ArrayList<Integer>();
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            aList.add(MJSpecialEnum.BAIBAN.value());
            mHashMap.put(4, aList);
        }
//		aList.clear();
        return mHashMap;
    }


    /**
     * 获取亮牌的列表
     * 注：只检查普通的牌型，其他特殊牌型需要特殊判断;
     * 一些需要多次筛选的牌型可以多次调用该方法
     * 如：第一次筛选，将第一的结果放进来再次筛选;
     * 注：可以七对胡，同时可以普通胡，这种牌型需要先把听七对胡的筛选出来
     * 22, 22, 23, 23, 29, 29, 29, 31, 31, 32, 32, 33, 33 = 22, 23, 29
     *
     * @param privateCardList 玩家私有牌不含首牌
     * @param huCardTypes     可胡列表
     * @return
     */
    public List<Integer> getLiangPaiList(List<Integer> privateCardList, List<Integer> huCardTypes) {
        Map<Integer, List<List<String>>> map = Maps.newMap();
        for (Integer tingCard : huCardTypes) {
            List<Integer> allCardList = Lists.newArrayList(privateCardList);
            allCardList.add(tingCard);
            if (allCardList.size() % 3 == 0) {
                allCardList.add(0);
                allCardList.add(0);
            }
            List<String> huList = HuUtil.getInstance().findHuTypeList1(allCardList, 0);
            if (CollectionUtils.isNotEmpty(huList)) {
                map.put(tingCard, existTingCardList(huList));
            }
        }
        if (map.size() == huCardTypes.size()) {
            List<List<String>> tingCard = map.get(huCardTypes.get(0));
            // 筛选出每个胡类型都出现过的牌组，这些牌组是移除掉不影响胡牌的。
            List<String> uselessCardList = this.getUselessCardList(tingCard, map).stream().distinct().collect(Collectors.toList());
            if (CollectionUtils.isEmpty(uselessCardList)) {
                // 手上的牌全亮
                return privateCardList;
            } else {
                List<Integer> uselessCardIniList = null;
                if (map.size() == 1 && huCardTypes.size() == 1) {
                    // 只听一张牌的情况下，不要的牌
                    uselessCardIniList = tingCard.get(0).stream().map(k -> Arrays.stream(k.trim().split(",")).map(v -> Integer.parseInt(v)).collect(Collectors.toList())).filter(k -> !k.contains(huCardTypes.get(0))).flatMap(k -> k.stream()).collect(Collectors.toList());
                } else {
                    // 不要的牌
                    uselessCardIniList = uselessCardList.stream().map(k -> Arrays.stream(k.trim().split(",")).map(v -> Integer.parseInt(v)).collect(Collectors.toList())).flatMap(k -> k.stream()).collect(Collectors.toList());
                }
                // 筛选出要亮的牌
                return removeLeftCardId(uselessCardIniList, privateCardList);
            }
        } else {
            // 不符合亮牌规则
            return Collections.emptyList();
        }
    }

    /**
     * 获取没用的列表
     *
     * @param tingCard
     * @param map
     * @return
     */
    private List<String> getUselessCardList(List<List<String>> tingCard, Map<Integer, List<List<String>>> map) {
        // 没用的列表
        List<String> uselessCardList = Lists.newArrayList();
        // 遍历牌组列表
        for (List<String> deckList : tingCard) {
            //
            Map<Integer, String> countMap = Maps.newMap();
            // 获取牌组
            for (String deck : deckList) {
                // 检查这个牌组在听得牌中都有出现则为一组
                // 统计出现次数
                Set<Integer> count = new HashSet<>();
                for (Map.Entry<Integer, List<List<String>>> entrySet : map.entrySet()) {
                    // 检查听得这组牌有没有出现相同的牌组
                    List<List<String>> existList = entrySet.getValue().stream().filter(k -> k.contains(deck)).collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(existList)) {
                        break;
                    }
                    if (countMap.containsKey(entrySet.getKey())) {
                        String value = countMap.get(entrySet.getKey());
                        if (!existList.stream().anyMatch(k -> k.toString().equals(value))) {
                            continue;
                        }
                    } else {
                        countMap.put(entrySet.getKey(), existList.get(0).toString());
                    }
                    count.add(entrySet.getKey());
                }
                if (count.size() == map.size()) {
                    // 记录
                    uselessCardList.add(deck);
                }
            }
            if (CollectionUtils.isNotEmpty(uselessCardList)) {
                // 只要有一组就直接取出
                return uselessCardList;
            }
        }
        return uselessCardList;
    }


    /**
     * 胡牌的牌组
     *
     * @param huList 胡牌列表
     * @return
     */
    public List<List<String>> existTingCardList(List<String> huList) {
        return huList.stream().map(k -> Arrays.stream(k.trim().split(":")).map(v -> v.trim()).collect(Collectors.toList())).collect(Collectors.toList());
    }

    /**
     * 移除不要的指定牌组
     *
     * @return
     */
    public List<Integer> removeLeftCardId(List<Integer> cardList, List<Integer> privateTypeList1) {
        List<Integer> privateTypeList = Lists.newArrayList(privateTypeList1);
        for (int cardId : cardList) {
            // 创建迭代器
            Iterator<Integer> it = privateTypeList.iterator();
            int card = 0;
            // 循环遍历迭代器
            while (it.hasNext()) {
                card = it.next();
                if (card == cardId) {
                    it.remove();
                    break;
                }
            }
        }
        return privateTypeList;
    }


    public static void main(String[] args) {

//		for (;;) {
//			List<Integer> allCardInts = new ArrayList<Integer>();
//			allCardInts.add(21);
//			allCardInts.add(21);
//			allCardInts.add(47);
//			allCardInts.add(21);
//			allCardInts.add(47);
//			allCardInts.add(47);
//			allCardInts.add(47);
//			List<Integer> Jins = new ArrayList<Integer>();
//			Jins.add(13);
//
//			System.out.println(HuUtil.getInstance().checkBTJAll(allCardInts, Jins).toString());
//		}

        System.out.println(8 % 3);

    }


}
