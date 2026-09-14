package com.ddm.server.common.utils;
import org.apache.commons.lang3.RandomUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class CommMath {

    private static final String m_randomHexKeyBase = "0123456789abcdef";
    private static final String m_randomAlphamericBase = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static double FormatDouble(double d) {
        BigDecimal bg = BigDecimal.valueOf(d).setScale(2, RoundingMode.DOWN);
        return bg.doubleValue();
    }

    /**
     * 产生一个处于[min,max]之间的随机整数
     *
     * @return
     */
    public static int randomInt(int min, int max) {
        if (min == max) {
            return min;
        }
        return randomInt(max - min) + min;
    }


    /**
     * 产生一个处于[0,Delta]之间的随机整数
     *
     * @param iDelta
     * @return
     */
    public static int randomInt(int iDelta) {
        return Math.abs(Random.nextInt(iDelta + 1));
    }
    /**
     * 加法运算
     *
     * @param v1
     * @param v2 向下取整（保留两位小数）
     * @return
     */
    public static double addDouble(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.add(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 减法运算
     *
     * @param v1
     * @param v2 向下取整（保留两位小数）
     * @return
     */
    public static double subDouble(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.subtract(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 乘法运算
     *
     * @param v1
     * @param v2 向下取整（保留两位小数）
     * @return
     */
    public static long mulLong(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.multiply(p2).setScale(2, RoundingMode.DOWN).longValue();
    }


    /**
     * 乘法运算
     *
     * @param v1
     * @param v2 向下取整（保留两位小数）
     * @return
     */
    public static double mul(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.multiply(p2).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 除法运算
     *
     * @param v1
     * @param v2 向下取整（保留两位小数）
     * @return
     */
    public static double div(double v1, double v2) {
        Double m1 = Double.valueOf(Double.toString(v1));
        Double m2 = Double.valueOf(Double.toString(v2));
        BigDecimal p1 = BigDecimal.valueOf(m1);
        BigDecimal p2 = BigDecimal.valueOf(m2);
        return p1.divide(p2, 2, RoundingMode.DOWN).doubleValue();
    }

    /**
     * 数字排序
     * @param list
     * @param asc T 升序（1..2）,F降序（2..1）
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void getSort(List list, boolean asc) {
        // 字符串排序
        Collections.sort(list);
        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if (asc) {
                    return Integer.compare((Integer) o1, (Integer) o2);
                } else {
                    return Integer.compare((Integer) o2, (Integer) o1);
                }
            }
        });
    }

    private static double EARTH_RADIUS = 6378137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 产生一个处于[0,1)之间的随机浮点数
     *
     * @return
     */
    public static float randomFloat() {
        return (float) CommMath.randomInt(10000) / 10000f;
    }

    public static <T> T randomOne(List<T> data) {
        int index = randomInt(data.size() - 1);
        return data.get(index);
    }

    /**
     * rateList 含有各项的权重， totalRate是总权重
     *
     * @return 获取列表_rateList的下标编号
     */
    public static <T> List<T> getRandomListByList(List<T> _totalList, int cnt) {
        List<T> src = new ArrayList<T>(_totalList);
        List<T> ret = new ArrayList<T>();
        if (0 == src.size()) {
            return ret;
        }

        if (cnt > src.size()) {
            CommLogD.warn("getRandomListByList cnt > _totalList.size()");
        }

        while (cnt > 0 && src.size() > 0) {
            cnt -= 1;
            int index = randomInt(src.size() - 1);
            ret.add(src.remove(index));
        }

        return ret;
    }

    public static List<Integer> getRandomListByWeightList(List<Integer> origin, List<Integer> weight, int count) {
        if (origin.size() != weight.size()) {
            CommLogD.warn("getRandomListByWeightList valueArray.size != weightArray.size");
        }

        List<Integer> idx = getRandomIndexByRate(weight, count);
        List<Integer> ret = new ArrayList<>(idx.size());
        for (int i = 0; i < idx.size(); i++) {
            ret.add(origin.get(idx.get(i)));
        }
        return ret;
    }

    /**
     * List<Integer> 里可能出现有重复
     *
     * @param origin
     * @param weight
     * @param count
     * @return
     */
    public static List<Integer> getRepeatableRandList(List<Integer> origin, List<Integer> weight, int count) {
        if (origin.size() != weight.size()) {
            CommLogD.warn("getRandomListByWeightList valueArray.size != weightArray.size");
        }

        List<Integer> ret = new ArrayList<>(count);
        int index = 0;
        while (count > 0) {
            index = getRandomIndexByRate(weight);
            ret.add(origin.get(index));
            count -= 1;
        }
        return ret;
    }

    /**
     * rateList 含有各项的权重， totalRate是总权重
     *
     * @param _rateList
     * @param _totalRate
     * @return 获取列表_rateList的下标编号
     */
    public static int getRandomRateByList(List<Integer> _rateList, int _totalRate) {
        if (0 == _totalRate && _rateList.size() > 0) {
            return 0;
        }

        if (0 == _totalRate) {
            return -1;
        }

        int rand = randomInt(_totalRate - 1) + 1;

        for (int index = 0; index < _rateList.size(); index++) {
            int curRate = _rateList.get(index);
            if (rand <= curRate) {
                return index;
            }
            rand -= curRate;
        }
        return -1;
    }

    /**
     * 简单打散列表 非线程安全
     *
     * @param <T>
     * @param _src
     * @return
     */
    public static <T> ArrayList<T> getRandomList(ArrayList<T> _src) {
        ArrayList<T> temp = new ArrayList<>(_src);
        Collections.shuffle(temp);
        return temp;
    }

    /**
     * 简单打散列表 非线程安全
     *
     * @param <T>
     * @param _src
     * @return
     */
    public static <T> ArrayList<T> getRandomList(ArrayList<T> _src,int number) {
        ArrayList<T> temp = new ArrayList<>(_src);
        for (int i = 0;i<number;i++) {
           temp = getRandomList(_src);
        }
        return temp;
    }


    /**
     * rateList 含有各项的权重
     *
     * @param <T>
     * @param _rateList
     * @param _valueList
     * @return
     */
    public static <T> T getRandomValue(List<Integer> _rateList, List<T> _valueList) {

        if (_rateList == null) {
            CommLogD.error("getRandomValueByRate. _rareList is null");
            return null;
        }

        if (_valueList == null) {
            CommLogD.error("getRandomValueByRate. _valueList is null");
            return null;
        }

        int index = getRandomIndexByRate(_rateList);

        if (-1 == index) {
            return null;
        }
        if (index >= _valueList.size()) {
            if (_valueList.isEmpty()) {
                return null;
            } else {
                return _valueList.get(_valueList.size() - 1);
            }
        }
        return _valueList.get(index);
    }

    /**
     * rateList 含有各项的权重
     *
     * @param _rateList
     * @return
     */
    public static int getRandomIndexByRate(List<Integer> _rateList) {
        int rand = 0;

        for (Integer rate : _rateList) {
            rand += rate;
        }

        if (_rateList.size() > 0 && rand == 0) {
            return randomInt(_rateList.size() - 1);
        }

        return getRandomRateByList(_rateList, rand);
    }

    public static List<Integer> getRandomIndexByRate(List<Integer> _rateList, int count) {
        ArrayList<Integer> copy = new ArrayList<>(_rateList);
        ArrayList<Integer> ret = new ArrayList<>();
        int idx = 0;
        while (count > 0) {
            idx = getRandomIndexByRate(copy);
            ret.add(idx);
            copy.set(idx, 0);
            count -= 1;
        }
        return ret;
    }

    public static <T> List<T> getRandomListByCnt(List<T> src, int count) {
        ArrayList<T> copy = new ArrayList<>(src);
        // 补满待选对象
        int ct = count / src.size();
        while (ct > 0) {
            copy.addAll(src);
            ct -= 1;
        }

        copy = getRandomList(copy);
        return copy.subList(0, count);
    }

    /**
     * 生成长度为32的16进制字符串
     *
     * @return
     */
    public static String randomHex() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            int iIndex = Random.nextInt(m_randomHexKeyBase.length());
            sb.append(m_randomHexKeyBase.charAt(iIndex));
        }
        return sb.toString();
    }

    /**
     * 生成指定位数的由字母数字构成的随机字符串
     *
     * @param count
     * @return
     */
    public static String randString(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int iIndex = Random.nextInt(m_randomAlphamericBase.length());
            sb.append(m_randomAlphamericBase.charAt(iIndex));
        }

        return sb.toString();
    }

    /**
     * 顺子
     * @param numbers
     * @return
     */
    public static boolean isContinuous(List<Integer> numbers) {
        double sum = 0;//等差数列求和
        int count = 0;
        if(numbers==null || numbers.size() <= 0){
            return false;
        }
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i=0;i<numbers.size();i++){
            count += numbers.get(i);
            list.add(numbers.get(i));
        }
        int len = list.size();

        Collections.sort(list);
        int num = len == 5 ? 4:2;

        if(Math.abs(list.get(0)-list.get(len-1))>num){
            return false;
        }
        for(int i=0; i<len-1; i++){
            if(list.get(i).equals(list.get(i + 1))){
                return false;
            }
        }

        sum = Double.parseDouble(String.valueOf(len*(list.get(0)+list.get(len-1))/2.0));
        if (count != sum) {
            return false;
        }

        return true;
    }


    public static Map<Integer, Long> groupingBy(List<Integer> pCards) {
        Map<Integer, Long> map = pCards.stream().collect(Collectors.groupingBy(p -> p,Collectors.counting()));
        return map;
    }

    /**
     *
     * @param cardType 牌类型
     * @param idx 位置
     * @param chiList 吃列表
     * @return
     */
    public static List<Integer>  shunZi (List<Integer> pCards,int cardType,int idx,List<Integer> chiList) {
        List<Integer> cardInts = new ArrayList<Integer>();
        //如果 下标 和 手牌长度一致
        if (idx == pCards.size()) {
            return chiList;
        }
        //从指定的下标开始，遍历出所有手牌
        for (int i =idx,size = pCards.size();i<size;i++) {
            //如果 手牌中的类型 == 牌的类型
            if (pCards.get(i) == cardType || pCards.get(i) >39) {
                continue;
            }
            //如果 手牌中类型 不出现重复 并且 记录的牌数 < 2
            if (!cardInts.contains(pCards.get(i)) && cardInts.size() < 2) {
                //添加不重复的牌
                cardInts.add(pCards.get(i));
                //如果 记录牌数 == 2 结束循环
            } else if (cardInts.size() == 2) {
                break;
            }
        }
        idx++;
        //如果 记录牌数 == 2
        if (cardInts.size() == 2)  {
            //添加牌
            cardInts.add(cardType);
            //判断是否顺子
            if (CommMath.isContinuous(cardInts)) {
                //如果是否有重复的顺子
                if (!chiList.containsAll(cardInts)) {
                    return cardInts;
                }
                return shunZi(pCards,cardType,idx,chiList);
            }
        }
        return shunZi(pCards,cardType,idx,chiList);
    }

    /**
     * 判断是否有重复的元素
     * @param list
     * @return
     */
    public static boolean notHasSame(List<? extends Object> list) {
        if(null == list) {
            return false;
        }
        return list.size() == new HashSet<Object>(list).size();
    }

    /**
     * 判断是否有重复的元素
     * @param list
     * @return
     */
    public static boolean hasSame(List<? extends Object> list) {
        return !notHasSame(list);
    }

    /**
     * 通过经纬度获取距离(单位：米)
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double getDistance(double lat1, double lng1, double lat2,  double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        return s * EARTH_RADIUS;
    }

    /**
     * @Description: 在矩形内随机生成经纬度
     * @param MinLon：最小经度
     * 		  MaxLon： 最大经度
     *  	  MinLat：最小纬度
     * 		  MaxLat：最大纬度
     * @return @throws
     */
    public static Map<String, String> randomLonLat(double MinLon, double MaxLon, double MinLat, double MaxLat) {
        BigDecimal db = new BigDecimal(Math.random() * (MaxLon - MinLon) + MinLon);
        String lon = db.setScale(6, RoundingMode.HALF_UP).toString();// 小数后6位
        db = new BigDecimal(Math.random() * (MaxLat - MinLat) + MinLat);
        String lat = db.setScale(6, RoundingMode.HALF_UP).toString();
        Map<String, String> map = new HashMap<String, String>();
        map.put("J", lon);
        map.put("W", lat);
        return map;
    }

    @SuppressWarnings("static-access")
    public static Integer chanceSelect(Map<Integer, Integer> keyChanceMap) {
        if (keyChanceMap == null || keyChanceMap.size() == 0)
            return null;

        Integer sum = 0;
        for (Integer value : keyChanceMap.values()) {
            sum += value;
        }
        // 从1开始
        Integer rand = new Random().nextInt(sum) + 1;

        for (Map.Entry<Integer, Integer> entry : keyChanceMap.entrySet()) {
            rand -= entry.getValue();
            // 选中
            if (rand <= 0) {
                return entry.getKey();
            }
        }
        return 0;
    }

    /**
     * 获取一个概率
     * @param ratio 概率的值
     * @return
     */
    public static boolean isTrueDouble(double ratio) {
        double value = RandomUtils.nextDouble(0.1D,100D);
        if (value <=  ratio) {
            return true;
        }
        return false;
    }
}
