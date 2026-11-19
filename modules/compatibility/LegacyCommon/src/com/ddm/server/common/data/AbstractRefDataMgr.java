package com.ddm.server.common.data;

import BaseCommon.CommClass;
import BaseCommon.CommLog;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.data.ref.RefBase;
import com.ddm.server.common.utils.CommFile;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public abstract class AbstractRefDataMgr {

    private static AbstractRefDataMgr instance = null;

    protected AbstractRefDataMgr() {
        instance = this;
    }

    public static AbstractRefDataMgr getInstance() {
        return instance;
    }

    protected Map<Class<? extends RefBase>, RefContainer<?>> refs = new ConcurrentHashMap<>();

    private boolean isHotUpdate = false;

    @SuppressWarnings("unchecked")
    public static <T extends RefBase> T get(Class<T> clazz, long key) {
        T ret = null;
        if (instance.refs.containsKey(clazz)) {
            ret = (T) instance.refs.get(clazz).get(key);
        }
        if (ret == null) {
            CommLogD.warn("BaseRefDataMgr get clazz:{}, key:{} failed!!", clazz.getSimpleName(), key);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T extends RefBase> T getOrLast(Class<T> clazz, long key) {
        T ret = null;
        if (instance.refs.containsKey(clazz)) {
            ret = (T) instance.refs.get(clazz).get(key);
        }
        if (ret == null) {
            return getAll(clazz).last();
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T extends RefBase> RefContainer<T> getAll(Class<T> clazz) {
        if (!instance.refs.containsKey(clazz)) {
            instance.refs.put(clazz, new RefContainer<T>());
        }
        return (RefContainer<T>) instance.refs.get(clazz);
    }

    public static <T extends RefBase> int size(Class<T> clazz) {
        if (!instance.refs.containsKey(clazz)) {
            instance.refs.put(clazz, new RefContainer<T>());
        }
        return instance.refs.get(clazz).size();
    }

    /**
     * 服务器启动加载配置表
     */
    public void reload() {
        this.isHotUpdate = false;
        this.loadConfig();
    }

    /**
     * 加载配置表
     */
    private void loadConfig() {
        CommLogD.info("=======开始加载配表");
        checkDirects();
        renameRefFilesToLowcase(null);
        onCustomLoad();// 自定义加载
        CommLogD.info("=======配表加载结束");
    }


    public boolean updateReloadName(String reloadName) {
        if (StringUtils.isEmpty(reloadName)) {
            CommLog.error("=======更新加载配置表开始，表名：{null}=======");
            return false;
        }
        CommLog.info("=======更新加载配置表开始，表名：{}=======", reloadName);
        this.isHotUpdate = true;
        if (!this.checkDirects()) {
            return false;
        }
        if (!this.renameRefFilesToLowcase(reloadName)) {
            return false;
        }
        this.onCustomLoad(reloadName);
        CommLog.info("=======更新加载配置表结束，表名：{}=======", reloadName);
        return true;
    }

    public boolean checkDirects() {
        File path = new File(this.getRefPath());
        if (!path.exists() || !path.isDirectory()) {
            CommLogD.error("【【【Refdata文件夹地址(" + path.getAbsolutePath() + ")配置错误！！！】】】");
            if (isHotUpdate) {
                return false;
            } else {
                System.exit(-1);
            }
        }
        return true;
    }

    public boolean renameRefFilesToLowcase(String name) {
        File path = new File(this.getRefPath());
        File[] files = path.listFiles();
        for (File refFile : files) {
            if (!refFile.isFile()) {
                // 不是文件，跳过
                continue;
            }
            // 参数名存在
            if (StringUtils.isNotEmpty(name)) {
                if (!refFile.getName().toLowerCase().startsWith(name.toLowerCase())) {
                    continue;
                }
            }
            if (!refFile.getName().toLowerCase().endsWith(".json")) {
                // 不是配置文件，跳过
                continue;
            }
            File lowcaseFile = new File(refFile.getParentFile(), refFile.getName().toLowerCase());
            if (lowcaseFile.exists()) {
                // 已经是小写文件 || windows底下不区分则跳过
                continue;
            }
            refFile.renameTo(lowcaseFile);
        }
        return true;
    }


    protected abstract boolean assertAll();

    /**
     * 自动加载到refs字典
     *
     * @param reloadName 加载指定的文件
     */
    protected abstract boolean onCustomLoad(String reloadName);

    /**
     * 自动加载到refs字典
     */
    protected abstract void onCustomLoad();

    public abstract String getRefPath();

    public <T extends RefBase> void load(Class<T> clazz) {
        List<Class<?>> refdatas = CommClass.getAllClassByInterface(clazz, clazz.getPackage().getName());
        for (Class<?> cs : refdatas) {
            RefBase refdata = null;
            try {
                refdata = (RefBase) CommClass.forName(cs.getName()).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                CommLogD.error("onAutoLoad occured error:{}", e.getMessage());
            }
            if (null == refdata) {
                continue;
            }
            RefContainer<?> data = refs.get(cs);
            if (data == null) {
                data = new RefContainer<>();
                refs.put(refdata.getClass(), data);
            }
            this.loadByClazz(refdata.getClass(), data);
        }
    }


    @SuppressWarnings("unchecked")
    public <T extends RefBase> void loadByClazz(Class<? extends RefBase> clazz, RefContainer<T> data) {
        String name = clazz.getSimpleName();
        String path = String.format("%s%c%s.json", this.getRefPath(), File.separatorChar, name.substring(3).toLowerCase());
        Map<String, JsonObject> table = CommFile.GetTable(path, 2, 3);
        if (null == table || table.size() <= 0) {
            CommLog.error("loadByClazz null == table || table.size() <= 0");
            return;
        }
        this.loadByClazz(clazz, data, table);
    }


    /**
     * 加载指定的配置文件
     *
     * @param clazz      父类
     * @param reloadName 加载指定文件名
     * @param <T>
     */
    public <T extends RefBase> boolean load(Class<T> clazz, String reloadName) {
        List<Class<?>> refdatas = CommClass.getAllClassByInterface(clazz, clazz.getPackage().getName());
        // 文件名
        String name = clazz.getSimpleName();
        // 文件路径
        String path = null;
        for (Class<?> cs : refdatas) {
            name = cs.getSimpleName();
            if (!name.substring(3).toLowerCase().equals(reloadName.toLowerCase())) {
                // 跳过不一致的文件
                continue;
            }
            path = String.format("%s%c%s.json", this.getRefPath(), File.separatorChar, name.substring(3).toLowerCase());
            Map<String, JsonObject> table = CommFile.GetTable(path, 2, 3);
            // 检查并解析配置文件
            if (null == table || table.size() <= 0) {
                // gson解析错误
                CommLog.error("load null == table || table.size() <= 0");
                return false;
            }
            RefBase refdata = null;
            try {
                refdata = (RefBase) CommClass.forName(cs.getName()).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                CommLogD.error("onAutoLoad occured error:{}", e.getMessage());
            }
            if (null == refdata) {
                continue;
            }
            RefContainer<?> data = refs.get(cs);
            if (data == null) {
                data = new RefContainer<>();
                refs.put(refdata.getClass(), data);
            }
            return this.loadByClazz(refdata.getClass(), data, table);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T extends RefBase> boolean loadByClazz(Class<? extends RefBase> clazz, RefContainer<T> data, Map<String, JsonObject> table) {
        Gson gson = new Gson();
        T refBase = null;
        for (JsonObject s : table.values()) {
            refBase = gson.fromJson(s, (Class<T>) clazz);
            if (null != refBase) {
                refBase.Assert();
                refBase.AssertAll(data);
                data.put(refBase.getId(), refBase);
            }
        }
        return true;
    }

}
