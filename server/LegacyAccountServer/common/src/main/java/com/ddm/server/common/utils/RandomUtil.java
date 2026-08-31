package com.ddm.server.common.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * 根据概率获取事件值
 * @author zhujianming
 * @date 2021-09-03 09:18
 */
public class RandomUtil {
    /**
     * 根据概率获取随机key
     * @param totalWeight 总份额权重
     * @param chance 概率分布的map<key,份额权重>
     * @return
     */
    public static int getRandomCard(int totalWeight, Map<Integer,Integer> chance)
    {
        java.util.Random random = new java.util.Random();
        int randomNumber = totalWeight>0?random.nextInt(totalWeight):-1;
        int min = 0;
        int max = 0;
        for (Iterator iter = chance.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<Integer, Integer> entry  = (Map.Entry<Integer, Integer>)iter.next();
            max = max + entry.getValue();
            if (randomNumber >= min && randomNumber <= max){
                return entry.getKey();
            }
            min = max;
        }
        return 0;
    }
}
