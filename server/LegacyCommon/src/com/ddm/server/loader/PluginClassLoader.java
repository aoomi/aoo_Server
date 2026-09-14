package com.ddm.server.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件类加载器
 * Created by zhouyanhui on 16-6-1.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(PluginClassLoader.class);
    /**
     * 本地缓存jars文件
     */
    private List<JarURLConnection> cachedJarFiles = new ArrayList<JarURLConnection>();

    private static URLClassLoader classloader = (URLClassLoader) ClassLoader.getSystemClassLoader();

    public PluginClassLoader() {
        super(new URL[]{}, findParentClassLoader());
    }

    static {
        // 1.7之后可以直接调用close方法关闭打开的jar，需要判断当前运行的环境是否支持close方法，如果不支持，需要缓存，避免卸载模块后无法删除jar
        try {
            URLClassLoader.class.getMethod("close");
        } catch (NoSuchMethodException e) { /* exception-policy: runtime capability probe may fail and uses legacy fallback */ } catch (SecurityException e) { /* exception-policy: runtime capability probe may fail and uses legacy fallback */ }
    }

    /**
     * Adds a directory to the class loader.
     *
     * @param directory       the directory.
     * @param developmentMode true if the plugin is running in development mode.
     *                        This resolves classloader conflicts between the
     *                        deployed plugin and development classes.
     */
    public void addDirectory(File directory, boolean developmentMode) {
        try {
            // Add classes directory to classpath.
            File classesDir = new File(directory, "classes");
            if (classesDir.exists()) {
                addURL(classesDir.toURI().toURL());
            }

            // Add i18n directory to classpath.
            File databaseDir = new File(directory, "database");
            if (databaseDir.exists()) {
                addURL(databaseDir.toURI().toURL());
            }

            // Add i18n directory to classpath.
            File i18nDir = new File(directory, "i18n");
            if (i18nDir.exists()) {
                addURL(i18nDir.toURI().toURL());
            }

            // Add web directory to classpath.
            File webDir = new File(directory, "web");
            if (webDir.exists()) {
                addURL(webDir.toURI().toURL());
            }

            // Add lib directory to classpath.
            File libDir = new File(directory, "lib");
            File[] jars = libDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
            });
            if (jars != null) {
                for (int i = 0; i < jars.length; i++) {
                    if (jars[i] != null && jars[i].isFile()) {
                        String jarFileUri = jars[i].toURI().toString() + "!/";
                        if (developmentMode) {
                            // Do not add plugin-pluginName.jar to classpath.
                            if (!jars[i].getName().equals("plugin-" + directory.getName() + ".jar")) {
                                addURLFile(new URL("jar", "", -1, jarFileUri));
                            }
                        } else {
                            addURLFile(new URL("jar", "", -1, jarFileUri));
                        }
                    }
                }
            }
        } catch (MalformedURLException mue) {
            logger.error(mue.getMessage(), mue);
        }
    }

    /**
     * Add the given URL to the classpath for this class loader, caching the JAR
     * file connection so it can be unloaded later
     *
     * @param file URL for the JAR file or directory to append to classpath
     */
    public void addURLFile(URL file) {
        try {
            // open and cache JAR file connection
            URLConnection uc = file.openConnection();
            if (uc instanceof JarURLConnection) {
                uc.setUseCaches(true);
                JarURLConnection urlC = ((JarURLConnection) uc);
                urlC.getManifest();
                cachedJarFiles.add((JarURLConnection) uc);
                //加载进内存

                //addURL(file);

                Method add = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{
                        URL.class});
                add.setAccessible(true);
                add.invoke(classloader, new Object[]{
                        file.toURI().toURL()});


            }
        } catch (Exception e) {
            logger.info("Failed to cache plugin JAR file: " + e);
            logger.warn("Failed to cache plugin JAR file: " + file.toExternalForm());
        }
    }

    /**
     * Unload any JAR files that have been cached by this plugin
     */
    public void unloadJarFiles() {
        close();
    }

    /**
     * Locates the best parent class loader based on context. 根据上下文定位最佳父类加载器。
     * 根据上下文定位最佳父类加载器。
     *
     * @return the best parent classloader to use.
     */
    private static ClassLoader findParentClassLoader() {
        ClassLoader parent = PluginManager.class.getClassLoader();
        if (parent == null) {
            parent = PluginClassLoader.class.getClassLoader();
        }
        if (parent == null) {
            parent = ClassLoader.getSystemClassLoader();
        }
        return parent;
    }

    @Override
    public void close() {
        for (JarURLConnection jarURLConnection : cachedJarFiles) {
            if (jarURLConnection == null) {
                return;
            }
            try {
                jarURLConnection.getJarFile().close();
                jarURLConnection = null;
                //System.gc();
                logger.info("Unloading plugin JAR file " + jarURLConnection.getJarFile().getName());
            } catch (Exception e) {
                logger.error("Failed to unload JAR file\n" + e);
            }
        }
    }
}
