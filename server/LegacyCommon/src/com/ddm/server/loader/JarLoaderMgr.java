package com.ddm.server.loader;

import BaseCommon.CommLog;
import com.ddm.server.common.CommLogD;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Jar加载、卸载管理
 *
 * @author Administrator
 */
public class JarLoaderMgr {

    // 类级的内部类，也就是静态的成员式内部类，该内部类的实例与外部类的实例 没有绑定关系，而且只有被调用到才会装载，从而实现了延迟加载
    private static class SingletonHolder {
        // 静态初始化器，由JVM来保证线程安全
        private static JarLoaderMgr instance = new JarLoaderMgr();
    }

    // 私有化构造方法
    private JarLoaderMgr() {
    }

    // 获取单例
    public static JarLoaderMgr getInstance() {
        return SingletonHolder.instance;
    }

    // jar加载的缓存。
    private final static ConcurrentHashMap<String, URLClassLoader> LOADER_CACHE = new ConcurrentHashMap<>();
    // 加载jar消息头字典
    private final static ConcurrentHashMap<String, Set<String>> loadeJarHandlerMap = new ConcurrentHashMap<>();

    /**
     * 添加加载jar消息头
     *
     * @param name    游戏名
     * @param handler 消息头
     */
    private void addLoadeJarHandler(String name, String handler) {
        // 检查key是否存在
        if (loadeJarHandlerMap.containsKey(name)) {
            // 添加指定key的列表中
            loadeJarHandlerMap.get(name).add(handler);
        } else {
            // 新增消息列表
            Set<String> handlerSet = new LinkedHashSet<>();
            handlerSet.add(handler);
            // 新增消息
            loadeJarHandlerMap.put(name, handlerSet);
        }

    }

    /**
     * 执行加载指定目录下jar包
     *
     * @param dir 指定目录文件
     * @throws IOException
     */
    public void execLoadeJarDirectory(File dir) throws IOException {
        // 检查是否目录
        if (dir.isDirectory()) {
            // 遍历目录下的所有文件或文件夹
            for (File file : dir.listFiles()) {
                // 执行加载jar
                this.execLoadeJarDirectory(file);
            }
        } else {
            // 加载指定jar包
            this.execLoadeJar(dir);
        }
    }

    /**
     * 执行加载指定jar包
     *
     * @param dir 指定jar文件
     * @throws IOException
     */
    public void execLoadeJar(File dir) throws IOException {
        // 检查文件是否存在。
        if (null == dir) {
            // 文件不存在。
            CommLogD.error("file null == dir");
            return;
        }
        // Loader callers may pass a classpath directory. It is not an invalid JAR and
        // must not create a production ERROR during otherwise healthy startup.
        if (dir.isDirectory()) {
            return;
        }
        // 获取文件名
        String fileName = dir.getName();
        // 检查文件后缀
        if (fileName.endsWith(".jar")) {
            // 获取文件前缀名
            String name = this.name(fileName);
            URLClassLoader urlClassLoader = LOADER_CACHE.get(name);
            if (null != urlClassLoader) {
                // 指定的jar包已经加入了。
                CommLogD.error("LOADER_CACHE exist :{}", name);
                return;
            }
            CommLog.info("execLoadeJar jar name : {}", name);
            // 添加到缓存中
            LOADER_CACHE.put(name, this.loadeJar(dir));
            this.getClassNameByJar(new JarFile(dir), name, String.format(System.getProperty("Server.Handler", "core/network/client2game/handler/%s"), name).toLowerCase(), false);
        } else {
            // 不是.jar的包。
            CommLogD.warn("skip non-jar classpath entry :{}", fileName);
        }
    }

    /**
     * 从jar获取某包下所有类
     *
     * @param jarPath      jar文件路径
     * @param packagePath  包路径
     * @param childPackage 是否遍历子包
     * @return 类的完整名称
     */
    private void getClassNameByJar(JarFile jarFile, String name, String packagePath, boolean childPackage) {
        try {
            Enumeration<JarEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                JarEntry jarEntry = entrys.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    if (childPackage) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
//							this.handlers.add(entryName);
                            this.addLoadeJarHandler(name, entryName);
                        }
                    } else {
                        int index = entryName.lastIndexOf("/");
                        String myPackagePath;
                        if (index != -1) {
                            myPackagePath = entryName.substring(0, index);
                        } else {
                            myPackagePath = entryName;
                        }
                        if (myPackagePath.equals(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
//							this.handlers.add(entryName);
                            this.addLoadeJarHandler(name, entryName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            CommLogD.error("JarLoaderMgr ClassNameByJar[{}] error:{} ", e.getMessage(), e);
        }
    }

    /**
     * 加载指定jar包
     *
     * @param jarName
     * @throws MalformedURLException
     */
    private URLClassLoader loadeJar(File jarFile) throws MalformedURLException {
        URL url = jarFile.toURI().toURL();
        return new URLClassLoader(new URL[]{url}, ClassLoader.getSystemClassLoader());
    }

    /**
     * 执行卸载指定目录下jar包
     *
     * @param dir 指定目录文件
     * @throws MalformedURLException
     */
    public void execUnloadJarDirectory(File dir) throws MalformedURLException {
        // 检查是否目录
        if (dir.isDirectory()) {
            // 遍历目录下的所有文件或文件夹
            for (File file : dir.listFiles()) {
                // 执行卸载jar
                this.execUnloadJarDirectory(file);
            }
        } else {
            // 卸载指定jar包
            this.execUnloadJar(dir);
        }
    }

    /**
     * 执行卸载指定jar包
     *
     * @param dir 指定jar文件
     * @throws MalformedURLException
     */
    public void execUnloadJar(File dir) throws MalformedURLException {
        // 检查文件是否存在。
        if (null == dir) {
            // 文件不存在。
            CommLogD.error("file null == dir");
            return;
        }
        // 获取文件名
        String fileName = dir.getName();
        // 获取文件前缀名
        String name = this.name(fileName);
        // 获取缓存对象
        URLClassLoader urlClassLoader = LOADER_CACHE.get(name);
        if (null == urlClassLoader) {
            CommLogD.error("null == urlClassLoader fileName:{}", name);
            return;
        }
        try {
            urlClassLoader.close();
        } catch (IOException e) {
            CommLogD.error("close jar classloader error:{}", e.getMessage(), e);
        }
        // 检查是否执行卸载操作。
        LOADER_CACHE.remove(name);
    }

    /**
     * 获取玩家前缀名
     *
     * @param name
     * @return
     */
    private String name(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * jar加载缓存keys
     *
     * @return
     */
    public Enumeration<String> jarLoaderKeys() {
        return LOADER_CACHE.keys();
    }

    /**
     * 获取头部列表
     *
     * @return
     */
    public Set<String> getHandlers() {
        return loadeJarHandlerMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    /**
     * 获取加载成功jar指定key消息头列表
     *
     * @param name 指定包名
     * @return
     */
    public Set<String> getloadeJarHandlerKeyList(String name) {
        return loadeJarHandlerMap.get(name);
    }

    /**
     * 检查是否有指定加载游戏的包
     *
     * @param name
     * @return
     */
    public boolean loadeJarContainsKey(String name) {
        return loadeJarHandlerMap.containsKey(name);
    }

}
