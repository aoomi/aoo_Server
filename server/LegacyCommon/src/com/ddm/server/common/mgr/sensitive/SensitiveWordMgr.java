/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ddm.server.common.mgr.sensitive;

import com.alibaba.fastjson.JSONObject;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommFile;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class SensitiveWordMgr {

    @SuppressWarnings("rawtypes")
    private Map sensitiveWordMap = new ConcurrentHashMap();
    public static final int minMatchTYpe = 1; // 最小匹配规则
    public static final int MaxMatchType = 2; //最大匹配规则

    private static class SingletonHolder {
        private static final SensitiveWordMgr INSTANCE = new SensitiveWordMgr();
    }

    public static SensitiveWordMgr getInstance() {
        return SingletonHolder.INSTANCE;
    }

    String sensitiveWordFileName = "Keywords.json";

    public void init(String filename) {
        sensitiveWordFileName = filename;
    }

    public void reload() {
        JSONObject keywordsJson = JSONObject.parseObject(CommFile.getTextFromFile(sensitiveWordFileName));
        buildWordsHashMap(keywordsJson);
        CommLogD.debug("total {} sensitive words loaded, and build {} words", keywordsJson.size(), sensitiveWordMap.size());
    }

    public int getSensitiveWordCount() {
        return sensitiveWordMap.size();
    }

    @SuppressWarnings("rawtypes")
    public Set getSensitiveWords() {
        return sensitiveWordMap.keySet();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void buildWordsHashMap(JSONObject jsonObject1) {
        sensitiveWordMap = new ConcurrentHashMap(jsonObject1.size()); // 初始化敏感词容器，减少扩容操作
        Map nowMap;
        Map<String, String> newWorMap;
        // 迭代keyWordSet
        for (Map.Entry<String, Object> entry : jsonObject1.entrySet()) {
            nowMap = sensitiveWordMap;
            for (int i = 0; i < entry.getKey().length(); i++) {
                char keyChar = entry.getKey().charAt(i); // 转换成char型
                Object wordMap = nowMap.get(keyChar); // 获取

                if (wordMap != null) { // 如果存在该key，直接赋值
                    nowMap = (Map) wordMap;
                } else { // 不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
                    newWorMap = new ConcurrentHashMap<>();
                    newWorMap.put("isEnd", "0"); // 不是最后一个
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }
                if (i == entry.getKey().length() - 1) {
                    nowMap.put("isEnd", "1"); // 最后一个
                }
            }
        }
    }

    /**
     * 以最小匹配规则判断文字是否包含敏感字符
     *
     * @param txt 文字
     * @return 若包含返回true，否则返回false
     * @version 1.0
     */
    public boolean isContainsSensitiveWord(String txt) {
        return isContainsSensitiveWord(txt, 1);
    }

    /**
     * 判断文字是否包含敏感字符
     *
     * @param txt       文字
     * @param matchType 匹配规则&nbsp;1：最小匹配规则，2：最大匹配规则
     * @return 若包含返回true，否则返回false
     * @author chenming
     * @date 2014年4月20日 下午4:28:30
     * @version 1.0
     */
    public boolean isContainsSensitiveWord(String txt, int matchType) {
        Pattern pName = Pattern.compile("[~`!@#$%\\^&\\*()-+_=<>?\\\\\\.:'\"{}\\[\\]]");
        Matcher matcherName = pName.matcher(txt);
        if (matcherName.find()) {
            return false;
        }

        boolean flag = false;
        for (int i = 0; i < txt.length(); i++) {
            int matchFlag = this.CheckSensitiveWord(txt, i, matchType); // 判断是否包含敏感字符
            if (matchFlag > 0) { // 大于0存在，返回true
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 获取文字中的敏感词
     *
     * @param txt       文字
     * @param matchType 匹配规则&nbsp;1：最小匹配规则，2：最大匹配规则
     * @return
     * @author chenming
     * @date 2014年4月20日 下午5:10:52
     * @version 1.0
     */
    public Set<String> getSensitiveWord(String txt, int matchType) {
        Set<String> sensitiveWordList = new HashSet<>();

        for (int i = 0; i < txt.length(); i++) {
            int length = CheckSensitiveWord(txt, i, matchType); // 判断是否包含敏感字符
            if (length > 0) { // 存在,加入list中
                sensitiveWordList.add(txt.substring(i, i + length));
                i = i + length - 1; // 减1的原因，是因为for会自增
            }
        }

        return sensitiveWordList;
    }

    /**
     * 替换敏感字字符
     *
     * @param txt
     * @param matchType   匹配规则&nbsp;1：最小匹配规则，2：最大匹配规则
     * @param replaceChar 替换字符，默认
     * @return
     * @author chenming
     * @date 2014年4月20日 下午5:12:07
     * @version 1.0
     */
    public String replaceSensitiveWord(String txt, int matchType, String replaceChar) {
        String resultTxt = txt;
        Set<String> set = getSensitiveWord(txt, matchType); // 获取所有的敏感词
        Iterator<String> iterator = set.iterator();
        String word = null;
        String replaceString = null;
        while (iterator.hasNext()) {
            word = iterator.next();
            replaceString = getReplaceChars(replaceChar, word.length());
            resultTxt = resultTxt.replaceAll(word, replaceString);
        }

        return resultTxt;
    }

    public String replaceSensitiveWordMax(String txt) {
        return replaceSensitiveWord(txt, MaxMatchType, "*");
    }

    public String replaceSensitiveWordMin(String txt) {
        return replaceSensitiveWord(txt, minMatchTYpe, "*");
    }

    /**
     * 获取替换字符串
     *
     * @param replaceChar
     * @param length
     * @return
     * @author chenming
     * @date 2014年4月20日 下午5:21:19
     * @version 1.0
     */
    private String getReplaceChars(String replaceChar, int length) {
        String resultReplace = replaceChar;
        for (int i = 1; i < length; i++) {
            resultReplace += replaceChar;
        }

        return resultReplace;
    }

    /**
     * 检查文字中是否包含敏感字符，检查规则如下：<br>
     *
     * @param txt
     * @param beginIndex
     * @param matchType
     * @author chenming
     * @date 2014年4月20日 下午4:31:03
     * @return，如果存在，则返回敏感词字符的长度，不存在返回0
     * @version 1.0
     */
    @SuppressWarnings({"rawtypes"})
    public int CheckSensitiveWord(String txt, int beginIndex, int matchType) {
        boolean flag = false; // 敏感词结束标识位：用于敏感词只有1位的情况
        int matchFlag = 0; // 匹配标识数默认为0
        char word = 0;
        Map nowMap = sensitiveWordMap;
        for (int i = beginIndex; i < txt.length(); i++) {
            word = txt.charAt(i);
            nowMap = (Map) nowMap.get(word); // 获取指定key
            if (nowMap != null) { // 存在，则判断是否为最后一个
                matchFlag++; // 找到相应key，匹配标识+1
                if ("1".equals(nowMap.get("isEnd"))) { // 如果为最后一个匹配规则,结束循环，返回匹配标识数
                    flag = true; // 结束标志位为true
                    if (minMatchTYpe == matchType) { // 最小规则，直接返回,最大规则还需继续查找
                        break;
                    }
                }
            } else { // 不存在，直接返回
                break;
            }
        }
        if (matchFlag < 1 || !flag) { // 长度必须大于等于1，为词
            matchFlag = 0;
        }
        return matchFlag;
    }
}
