package com.ddm.server.common.data.ref;

import com.ddm.server.common.data.RefContainer;

import java.io.Serializable;

/**
 *
 */
public abstract class RefBase implements Serializable {

    /**
     * 支持基本类型 public int _int; public int _long; public String _str; public
     * boolean _bool; 支持数组 (非简单类型) public Integer[] _ints; 支持List<>嵌套 public
     * ArrayList<String> _sList; 支持本路径下的其他结构体 public RefData _ref;
     * 支持ConstEnum.java里的枚举 public TestEnum _enum;
     */

    /**
     * 进行断言，发现问题配表存在问题时终止服务器启动
     *
     * @return FALSE 终止启动，TRUE 校验通过
     */
    public abstract boolean Assert();

    /**
     * 进行全局断言，发现问题配表存在问题时终止服务器启动
     *
     * @param all 已经读取上来的所有配表信息
     * @return FALSE 终止启动，TRUE 校验通过
     */
    public abstract boolean AssertAll(RefContainer<?> all);

    public abstract long getId();


    @Override
    public String toString() {
        return super.toString();
    }

    public String[] getOptionFields() {
        return new String[0];
    }

    public boolean isFieldRequired(String filed) {
        for (String f : getOptionFields()) {
            if (f.equals(filed)) {
                return false;
            }
        }
        return true;
    }

}
