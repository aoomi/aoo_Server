package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;


/**
 * 管理加载的properties文件
 *
 * @author Deniulor
 */
public class CommProperties {

    private static Set<String> loadedProperties = new HashSet<>();

    /**
     * 从指定配置文件中读取properties配置
     *
     * @param file
     * @return
     */
    public static boolean load(String file) {
        return load(file, true);
    }

    public static boolean load(String file, boolean userlogger) {

        CommLogD.debug("用户的当前工作目录:{}", System.getProperty("user.dir"));

        // 输入有效则开始读取对应配置
        try (InputStream propertiesInputStream = new FileInputStream(file);
             BufferedReader bf = new BufferedReader(new InputStreamReader(propertiesInputStream, java.nio.charset.StandardCharsets.UTF_8));) {
            Properties properties = new Properties();
            properties.load(bf);
            Properties pre = System.getProperties();
            for (Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString().trim();
                String value = entry.getValue().toString().trim();
                if (value.endsWith(";")) {
                    value = value.substring(0, value.length() - 1);
                }
                String envKey = "aoo_" + key.toUpperCase().replaceAll("[^A-Z0-9]", "_");
                String envValue = System.getenv(envKey);
                if (envValue != null) {
                    value = envValue;
                }
                pre.setProperty(key, value);
            }
            CommProperties.loadedProperties.add(file);
        } catch (Exception e) {
            if (userlogger) {
                CommLogD.error("加载properties[{}]配置错误：", file, e);
            } else {
                CommLogD.error("加载properties[" + file + "]配置错误", e);
            }
            return false;
        }
        return true;
    }

    /**
     * 输出所有的读取过的配置文件信息
     */
    public static void printPropertis() {
        for (String filepath : loadedProperties) {
            CommLogD.info("");
            CommLogD.info("print properties in file:{}", filepath);
            try (BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), java.nio.charset.StandardCharsets.UTF_8))) {
                Properties properties = new Properties();
                properties.load(bf);
                for (Entry<Object, Object> entry : properties.entrySet()) {
                    String key = entry.getKey().toString().trim();
                    String value = entry.getValue().toString().trim();
                    if (value.endsWith(";")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    String normalizedKey = key.toLowerCase();
                    boolean sensitive = normalizedKey.contains("password")
                            || normalizedKey.contains("secret")
                            || normalizedKey.contains("privatekey")
                            || normalizedKey.endsWith("_key")
                            || normalizedKey.endsWith(".key");
                    CommLogD.info("property key:{}  value:{}", key, sensitive ? "[REDACTED]" : value);
                }
            } catch (Exception e) {
                CommLogD.error("file({}) load failed", e);

            }
        }
    }
}
