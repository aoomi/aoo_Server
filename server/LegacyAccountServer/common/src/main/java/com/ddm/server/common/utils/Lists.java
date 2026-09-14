/**
 * License THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). THE WORK IS PROTECTED
 * BY COPYRIGHT AND/OR OTHER APPLICABLE LAW. ANY USE OF THE WORK OTHER THAN AS
 * AUTHORIZED UNDER THIS LICENSE OR COPYRIGHT LAW IS PROHIBITED.
 *
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND AGREE TO
 * BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE MAY BE
 * CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 *
 */
package com.ddm.server.common.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lists {

    public static <E> List<E> newConcurrentList() {
        return new CopyOnWriteArrayList<>();
    }

    public static <E> List<E> newConcurrentList(Collection<? extends E> c) {
        return new CopyOnWriteArrayList<>(c);
    }

    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    public static <E> ArrayList<E> newArrayList(Collection<? extends E> c) {
        return new ArrayList<>(c);
    }

    public static <E> LinkedList<E> newLinkedList() {
        return new LinkedList<>();
    }

    public static <E> LinkedList<E> newLinkedList(Collection<? extends E> c) {
        return new LinkedList<>(c);
    }

    public static <E> HashSet<E> newHashSet() {
        return new HashSet<>();
    }

    public static <E> HashSet<E> newHashSet(Collection<? extends E> c) {
        return new HashSet<>(c);
    }

    public static <E> ArrayList<E> newArrayList(E... elements) {
        Preconditions.checkNotNull(elements);
        int capacity = Lists.computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    @VisibleForTesting
    static int computeArrayListCapacity(int arraySize) {
        Preconditions.checkArgument(arraySize >= 0);
        return Ints.saturatedCast(5L + (long)arraySize + (long)(arraySize / 10));
    }

    /** 
     * 将一个list均分成n个list,主要通过偏移量来实现的 
     * @param source 
     * @return 
     */  
    public static <T> List<List<T>> averageAssign(List<T> source,int n){  
        List<List<T>> result=new ArrayList<List<T>>();  
        int remaider=source.size()%n;  //(先计算出余数)  
        int number=source.size()/n;  //然后是商  
        int offset=0;//偏移量  
        for(int i=0;i<n;i++){  
            List<T> value=null;  
            if(remaider>0){  
                value=source.subList(i*number+offset, (i+1)*number+offset+1);  
                remaider--;  
                offset++;  
            }else{  
                value=source.subList(i*number+offset, (i+1)*number+offset);  
            }  
            result.add(value);  
        }  
        return result;  
    }

    /**
     * 深度复制list对象,先序列化对象，再反序列化对象
     *
     * @param src 需要复制的对象列表
     * @return 返回新的对象列表
     * @throws IOException 读取Object流信息失败
     * @throws ClassNotFoundException 泛型类不存在
     */
    public static <T> List<T> deepCopy(List<T> src)
            throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        return (List<T>)in.readObject();
    }

    
}
