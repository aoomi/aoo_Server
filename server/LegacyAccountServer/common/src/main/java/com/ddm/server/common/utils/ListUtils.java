package com.ddm.server.common.utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    /**
     * 将一个list均分成n个list,主要通过偏移量来实现的
     * @param source
     * @return
     */
    public static <T> List<List<T>> AverageAssign(List<T> source, int n){
        List<List<T>> result=new ArrayList<List<T>>();
        int number=source.size()/n;  //然后是商
        for(int i=0;i<number;i++){
            result.add(source.subList(i*n, (i+1)* n));
        }
        return result;
    }
}
