package com.ddm.server.loader;

import BaseCommon.CommLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhouyanhui on 16-6-1.
 */
public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private PluginClassLoader loader = null;
    /**
     * 本地插件库
     */
    private Map<String, PluginClassLoader> pluginMap = new HashMap<String, PluginClassLoader>();

    public PluginManager() {

    }

    private void addLoader(String pluginName, PluginClassLoader loader) {
        this.pluginMap.put(pluginName, loader);
    }

    public PluginClassLoader getLoader(String pluginName) {
        return this.pluginMap.get(pluginName);
    }


    /**
     * 加载插件
     *
     * @param pluginurl  e.g.: "jar:file:/D:/testclassloader/" + pluginName + ".jar!/"
     * @param pluginName
     */
    public PluginManager loadPlugin(String pluginurl, String pluginName) {
        logger.debug("loading plugin {}", pluginName);
        this.pluginMap.remove(pluginName);
        loader = new PluginClassLoader();
        URL url = null;
        try {
            url = new URL(pluginurl);
        } catch (MalformedURLException e) {
            logger.error("load plugin {} url exception, pluginurl: {}", new Object[]{pluginName, pluginurl, e});
            throw new PluginException("load plugin " + pluginName + " failed", e);
        }
        try {
            loader.addURLFile(url);
            addLoader(pluginName, loader);
            logger.info("load plugin {} success", pluginName);
        } catch (Exception e) {
            logger.error("load plugin {} url exception, pluginurl: {}", new Object[]{pluginName, pluginurl, e});
            throw new PluginException("load plugin " + pluginName + " failed", e);
        }
        return this;
    }


    /**
     * 加载插件
     *
     * @param url  e.g.: "jar:file:/D:/testclassloader/" + pluginName + ".jar!/"
     * @param name
     */
    public void loadPlugin(URL url, String name) {
        this.pluginMap.remove(name);
        loader = new PluginClassLoader();

        try {
            loader.addURLFile(url);
            addLoader(name, loader);
            CommLog.info("load plugin {} success", name);
        } catch (Exception e) {
            CommLog.error("load plugin {} url exception, {}", new Object[]{name, e});
            throw new PluginException("load plugin " + name + " failed", e);
        }
    }

    /**
     * @param pluginName
     */
    public void unloadPlugin(String pluginName) {
        try {
            this.pluginMap.get(pluginName).unloadJarFiles();
            this.pluginMap.remove(pluginName);
            CommLog.info("unload plugin {} success", pluginName);
        } catch (Exception e) {
            CommLog.error("unload plugin {} url exception", pluginName, e);
            throw new PluginException("unload plugin " + pluginName + " failed", e);
        }
    }
}
