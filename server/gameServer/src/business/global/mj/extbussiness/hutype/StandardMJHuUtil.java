package business.global.mj.extbussiness.hutype;

import com.ddm.server.common.utils.CommString;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 麻将字牌做顺胡
 * 普通胡牌
 * 字牌做顺子
 * 顺子：所谓的顺子，是由三张连续的数字所组成，例如《一万-二万-三万》。
 * 	注：东南西北可组成顺子，中发白可组成顺子；
 *
 * @author Administrator
 */
public class StandardMJHuUtil {

    private volatile static StandardMJHuUtil singleton;

    private StandardMJHuUtil() {
    }

    private List<Integer> numList = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

    public static StandardMJHuUtil getInstance() {
        if (singleton == null) {
            synchronized (StandardMJHuUtil.class) {
                if (singleton == null) {
                    singleton = new StandardMJHuUtil();
                }
            }
        }
        return singleton;
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
     * 获取胡牌类型
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> findHuTypeList(List<Integer> allCards, int totalJin,boolean needZeroReplace) {
        allCards.sort(Comparator.comparingInt(o -> o));
        return checkPossibleGroup(allCards, totalJin,needZeroReplace);
    }

    /**
     * 获取胡牌类型
     *
     * @param allCards
     * @param totalJin
     * @return
     */
    public List<String> findHuTypeList1(List<Integer> allCards, int totalJin,boolean needZeroReplace) {
        allCards.sort(Comparator.comparingInt(o -> o));
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
                if (needZeroReplace) {
                    dui.add(type);
                } else {
                    dui.add(0);
                }
            }

            List<List<Integer>> matched = new ArrayList<>();
            //
            matchThird(resultList, 0, dui, matched, leftJin, leftCards,needZeroReplace);
        }
        //金做将牌
        if (totalJin > 0) {
            // 遍历每种牌
            int limit = totalJin >= 2 ? 2 : 1;
            for (int p = 1; p <= limit; p++) {
                List<Integer> dui = needZeroReplace ? new ArrayList<>() : new ArrayList<>(Collections.nCopies(p, 0));
                int leftJin = totalJin - p;
                if (p == 1) {
                    for (Integer type : typeList) {
                        List<Integer> leftCards = new ArrayList<>(allCards);
                        leftCards.remove(type);
                        if (needZeroReplace) {
                            dui.add(type);
                        }
                        dui.add(type);
                        List<List<Integer>> matched = new ArrayList<>();
                        matchThird(resultList, 0, dui, matched, leftJin, leftCards, needZeroReplace);
                        dui.remove(type);
                        if (needZeroReplace) {
                            dui.clear();
                        }
                    }
                } else {
                    dui = new ArrayList<>(Collections.nCopies(p, 0));
                    List<Integer> leftCards = new ArrayList<>(allCards);
                    List<List<Integer>> matched = new ArrayList<>();
                    matchThird(resultList, 0, dui, matched, leftJin, leftCards, needZeroReplace);
                }
            }
        }
        return resultList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 检查可能性组合
     *
     * @param allCards
     * @param totalJin
     * @param needZeroReplace
     * @return
     */
    public List<String> checkPossibleGroup(List<Integer> allCards, int totalJin, boolean needZeroReplace) {
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
                if (needZeroReplace) {
                    dui.add(type);
                } else {
                    dui.add(0);
                }
            }

            List<List<Integer>> matched = new ArrayList<>();
            //
            matchThird(resultList, 0, dui, matched, leftJin, leftCards, needZeroReplace);
        }
        return resultList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 胡
     *
     * @return
     */
    public boolean checkHu(List<Integer> allCards, int totalJin) {
        allCards.sort(Comparator.comparingInt(o -> o));
        return check3nP2(allCards, totalJin);
    }


    public boolean check3nP2(List<Integer> allCards, int totalJin) {
        if (allCards.size() <= 0 && totalJin > 0) {
            return true;
        }

        List<String> resultList = new ArrayList<>();
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
            matchThird(resultList, 0, dui, matched, leftJin, leftCards, true);
        }
        return resultList.size() > 0;
    }

    public void matchThird(List<String> resultList, int tabCnt,
                           List<Integer> dui, List<List<Integer>> matched, int leftJin,
                           List<Integer> leftCards, boolean needZeroReplace) {
        if (leftCards.size() == 0 && leftJin % 3 == 0) {
            String end = CommString.join(",", dui);
            for (List<Integer> match : matched) {
                end += "  :  " + CommString.join(",", match);
            }
            for (int i = 1; i <= leftJin / 3; i++) {
                end += "  :  " + "0,0,0";
            }
            resultList.add(end);
            return;
        }

        if (leftCards.size() == 0) {
            return;
        }
        // 取一个值
        Integer type = leftCards.get(0);
        leftCards.remove(type);
        numList.forEach(num -> {
            if (num == 0) {
                // 尝试匹配 3type
                List<Integer> newMatched = new ArrayList<>();
                List<Integer> newLeftCard = matchKe(new ArrayList<>(leftCards), type,
                        leftJin, newMatched);
                if (null != newLeftCard) {
                    List<List<Integer>> newMatchList = new ArrayList<>(matched);
                    int newLeftJin = leftJin - calcNum(newMatched, 0);
                    if (needZeroReplace) {
                        Optional<Integer> firstNotZero = newMatched.stream().filter(u -> u != 0).findFirst();
                        firstNotZero.ifPresent(integer -> {
                            newMatched.set(0, integer);
                            newMatched.set(1, integer);
                            newMatched.set(2, integer);
                        });
                    }
                    newMatchList.add(newMatched);
                    matchThird(resultList, tabCnt + 1, dui, newMatchList, newLeftJin,
                            newLeftCard, needZeroReplace);
                }
            } else if (num == 1) {
                // 尝试匹配 shun1
                List<Integer> newMatched1 = new ArrayList<>();
                List<Integer> newLeftCard1 = matchShunLeft(new ArrayList<>(leftCards),
                        type, leftJin, newMatched1);
                if (null != newLeftCard1) {
                    List<List<Integer>> newMatchList1 = new ArrayList<>(matched);
                    newMatchList1.add(newMatched1);
                    int newLeftJin = leftJin - calcNum(newMatched1, 0);
                    matchThird(resultList, tabCnt + 1, dui, newMatchList1, newLeftJin,
                            newLeftCard1, needZeroReplace);
                }
            } else if (num == 2) {
                // 尝试匹配 shun2
                List<Integer> newMatched2 = new ArrayList<>();
                List<Integer> newLeftCard2 = matchShunMid(new ArrayList<>(leftCards),
                        type, leftJin, newMatched2);
                if (null != newLeftCard2) {
                    List<List<Integer>> newMatchList2 = new ArrayList<>(matched);
                    newMatchList2.add(newMatched2);
                    int newLeftJin = leftJin - calcNum(newMatched2, 0);
                    matchThird(resultList, tabCnt + 1, dui, newMatchList2, newLeftJin,
                            newLeftCard2, needZeroReplace);
                }
            } else if (num == 3) {
                // 尝试匹配 shun3
                List<Integer> newMatched3 = new ArrayList<>();
                List<Integer> newLeftCard3 = matchShunRight(new ArrayList<>(leftCards),
                        type, leftJin, newMatched3);
                if (null != newLeftCard3) {
                    List<List<Integer>> newMatchList3 = new ArrayList<>(matched);
                    newMatchList3.add(newMatched3);
                    int newLeftJin = leftJin - calcNum(newMatched3, 0);
                    matchThird(resultList, tabCnt + 1, dui, newMatchList3, newLeftJin,
                            newLeftCard3, needZeroReplace);
                }
            }
        });

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
        if (type % 10 + 1 > 9 || type % 10 + 2 > 9) {
            return null;
        }

        if (type > 40) {
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
        if (type % 10 + 1 > 9 || type % 10 - 1 < 1) {
            return null;
        }

        if (type > 40) {
            return null;
        }


        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
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

        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchShunRight(List<Integer> srcCards, Integer type,
                                        int jin, List<Integer> newMatched) {
        if (type % 10 - 1 < 1 || type % 10 - 2 < 1) {
            return null;
        }

        if (type > 40) {
            return null;
        }
        Integer left2 = type - 2;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
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
        newMatched.add(type);
        return jin >= 0 ? srcCards : null;
    }


    public List<Integer> matchDaLeftShun1(List<Integer> srcCards, Integer type,
                                          int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }
        if (type + 2 > 44 || type + 3 > 44) {
            return null;
        }

        newMatched.add(type);
        Integer right1 = type + 2;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        Integer right2 = type + 3;
        if (srcCards.contains(right2)) {
            srcCards.remove(right2);
            newMatched.add(right2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }


    public List<Integer> matchDaLeftShun2(List<Integer> srcCards, Integer type,
                                          int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }
        if (type + 1 > 44 || type + 3 > 44) {
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

        Integer right2 = type + 3;
        if (srcCards.contains(right2)) {
            srcCards.remove(right2);
            newMatched.add(right2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaLeftShun3(List<Integer> srcCards, Integer type,
                                          int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }
        if (type + 1 > 44 || type + 2 > 44) {
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


    public List<Integer> matchDaShunMid1(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 1 > 44 || type - 1 < 41) {
            return null;
        }

        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
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
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid2(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 1 > 44 || type - 2 < 41) {
            return null;
        }

        Integer left1 = type - 2;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
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

        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid3(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 1 > 44 || type - 3 < 41) {
            return null;
        }

        Integer left1 = type - 3;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
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

        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid4(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 2 > 44 || type - 1 < 41) {
            return null;
        }

        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer right1 = type + 2;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid5(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 2 > 44 || type - 2 < 41) {
            return null;
        }

        Integer left1 = type - 2;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer right1 = type + 2;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid6(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 2 > 44 || type - 3 < 41) {
            return null;
        }

        Integer left1 = type - 3;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer right1 = type + 2;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid7(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 3 > 44 || type - 1 < 41) {
            return null;
        }

        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer right1 = type + 3;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunMid8(List<Integer> srcCards, Integer type,
                                         int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type + 3 > 44 || type - 2 < 41) {
            return null;
        }

        Integer left1 = type - 2;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer right1 = type + 3;
        if (srcCards.contains(right1)) {
            srcCards.remove(right1);
            newMatched.add(right1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunRight1(List<Integer> srcCards, Integer type,
                                           int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type - 2 < 41 || type - 3 < 41) {
            return null;
        }
        Integer left2 = type - 3;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        Integer left1 = type - 2;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunRight2(List<Integer> srcCards, Integer type,
                                           int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type - 1 < 41 || type - 3 < 41) {
            return null;
        }
        Integer left2 = type - 3;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
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
        newMatched.add(type);
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchDaShunRight3(List<Integer> srcCards, Integer type,
                                           int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 40 || type >= 45) {
            return null;
        }

        if (type - 1 < 41 || type - 2 < 41) {
            return null;
        }
        Integer left2 = type - 2;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
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
        newMatched.add(type);
        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchZFBShunRight1(List<Integer> srcCards, Integer type,
                                            int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 45 || type > 47) {
            return null;
        }

        if (type - 1 < 45 || type - 2 < 45) {
            return null;
        }
        Integer left2 = type - 2;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
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
        newMatched.add(type);

        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchZFBShunRight2(List<Integer> srcCards, Integer type,
                                            int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 45 || type > 47) {
            return null;
        }

        if (type - 1 < 45 || type + 1 > 47) {
            return null;
        }

        Integer left1 = type - 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }
        newMatched.add(type);
        Integer left2 = type + 1;
        if (srcCards.contains(left2)) {
            srcCards.remove(left2);
            newMatched.add(left2);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        return jin >= 0 ? srcCards : null;
    }

    public List<Integer> matchZFBShunRight3(List<Integer> srcCards, Integer type,
                                            int jin, List<Integer> newMatched) {
        // 如果 < 40 不是大牌，或者 >= 45 ，不是 中发白
        if (type < 45 || type > 47) {
            return null;
        }

        if (type + 2 > 47 || type + 1 > 47) {
            return null;
        }

        newMatched.add(type);
        Integer left1 = type + 1;
        if (srcCards.contains(left1)) {
            srcCards.remove(left1);
            newMatched.add(left1);
        } else {
            jin -= 1;
            newMatched.add(0);
        }

        Integer left2 = type + 2;
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


    public static void main(String[] args) {

//		16 16 21 22 23 25 26 27 41 42 43 44 44 44
        List<Integer> allCards = new ArrayList<>();
        allCards.add(13);
        allCards.add(14);
        allCards.add(15);


        allCards.add(37);
        allCards.add(35);
        allCards.add(36);

        allCards.add(41);
        allCards.add(41);
        allCards.add(42);
        allCards.add(42);

        allCards.add(44);
        allCards.add(44);
        allCards.add(16);
        allCards.add(16);


        if (StandardMJHuUtil.getInstance().checkHu(allCards, 0)) {
            System.out.println("OK");
        } else {
            System.out.println("NO");
        }


    }

    public boolean checkBaiBanHuCard(List<Integer> allCardInts, int totalJin, List<Integer> Jins) {
        return check3nP2(allCardInts, totalJin);
    }

}
