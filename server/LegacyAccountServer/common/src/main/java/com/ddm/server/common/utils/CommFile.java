package com.ddm.server.common.utils;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.netty.util.NetUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;


public class CommFile {

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
                CommLogD.error(NetUtil.class.getName(), e);
            }
        }
    }

    /**
     * 读文件
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static String bufferedReader(String path) throws IOException {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException("file not exist:" + path);
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            CommLogD.error("bufferedReader：" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return sb.toString();
    }

    /**
     * 读文件, 解决编码问题
     *
     * @param path
     * @param code
     * @return
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static String BufferedReaderEncode(String path, String code) throws IOException {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException();
        }
        StringBuilder sb = new StringBuilder();
        FileInputStream fr = null;
        BufferedReader br = null;
        try {
            fr = new FileInputStream(path);
            br = new BufferedReader(new InputStreamReader(fr, code));
            String temp = br.readLine();
            while (null != temp) {
                sb.append(temp).append(" ");
                temp = br.readLine();
            }
        } catch (Exception e) {
            CommLogD.error("BufferedReaderEncode" + e.getMessage());
        } finally {
            if (br != null) {
                br.close();
            }
            if (fr != null) {
                fr.close();
            }
        }
        return sb.toString();
    }


    /**
     * 将文件内容解析为二维表
     *
     * @param path
     * @param fieldLine   字段所在行(首行从1算起)
     * @param contentLine 正文起始行
     * @return
     */
    public static Map<String, JsonObject> GetTable(String path, int fieldLine, int contentLine) {
        Map<String, JsonObject> table = new HashMap<>();
        try {
            File file = new File(path);
            if (!file.exists() || file.isDirectory()) {
                CommLogD.error("file not exist:{} ", path);
                return table;
            }

            StringBuilder result = new StringBuilder();
            InputStreamReader read = null;// 考虑到编码格式
            BufferedReader br = null;//构造一个BufferedReader类来读取文件
            try {
                read = new InputStreamReader(new FileInputStream(file), "utf-8");// 考虑到编码格式
                br = new BufferedReader(read);//构造一个BufferedReader类来读取文件
                String s = null;
                while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                    result.append(System.lineSeparator() + s);
                }
            } catch (Exception e) {
                CommLogD.error("txtFile to String error fileName:" + file.getName());
                CommLogD.error("File operation failed", e);
            } finally {
                if (br != null) {
                    br.close();
                }
                if (read != null) {
                    read.close();
                }
            }
            String resultStr = result.toString();
            if (StringUtils.isEmpty(resultStr)) {
                CommLogD.error("resultStr not exist:{} ", resultStr);
                return table;
            }
            table = GsonUtils.stringToBean(resultStr, new TypeToken<Map<String, JsonObject>>() {
            }.getType());
        } catch (Exception e) {
            CommLogD.error("GetTable:" + e.getMessage());
        }
        return table;
    }


    /**
     * 将文件内容解析为二维表
     *
     * @param <T>
     * @param path
     * @param type
     * @return
     */
    public static <T> Map<Integer, T> GetJsonData(String path, Type type) {
        Map<Integer, T> table = new HashMap<>();
        try {
            File file = new File(path);
            if (!file.exists() || file.isDirectory()) {
                CommLogD.error("file not exist:{} ", path);
                return table;
            }

            StringBuilder result = new StringBuilder();
            InputStreamReader read = null;// 考虑到编码格式
            BufferedReader br = null;//构造一个BufferedReader类来读取文件
            try {
                read = new InputStreamReader(new FileInputStream(file), "utf-8");// 考虑到编码格式
                br = new BufferedReader(read);//构造一个BufferedReader类来读取文件
                String s = null;
                while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                    result.append(System.lineSeparator() + s);
                }
            } catch (Exception e) {
                CommLogD.error("txtFile to String error fileName:" + file.getName());
                CommLogD.error("File operation failed", e);
            } finally {
                if (br != null) {
                    br.close();
                }
                if (read != null) {
                    read.close();
                }
            }
            String resultStr = result.toString();
            if (StringUtils.isEmpty(resultStr)) {
                CommLogD.error("resultStr not exist:{} ", resultStr);
                return table;
            }
            table = GsonUtils.stringToBean(resultStr, type);
        } catch (Exception e) {
            CommLogD.error("GetJsonData:" + e.getMessage());
        }

        return table;
    }


    /**
     * 将文件内容解析为行表
     *
     * @param path
     * @param fieldLine   字段所在行(首行从1算起)
     * @param contentLine 正文起始行
     * @return
     */
    public static List<Map<String, String>> GetRowSet(String path, int fieldLine, int contentLine) {
        List<Map<String, String>> lines = new ArrayList<>();
        Map<String, Map<String, String>> table = new ConcurrentHashMap<>();

        File file = new File(path);
        if (!file.exists() || file.isDirectory() || fieldLine >= contentLine || fieldLine <= 0 || contentLine <= 0) {
            return lines;
        }

        Charset cs = Charset.forName("utf-8");
        BufferedReader br = null;
        try {
            FileInputStream fr = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(fr, cs));
        } catch (FileNotFoundException e) {
            CommLogD.error(null, e);
        }
        if (null == br) {
            return lines;
        }

        String temp = null;

        String[] fields = null;
        int curLine = 0;
        do {
            try {
                temp = br.readLine();
                if (null == temp) {
                    break;
                }

                temp = temp.trim();
                curLine += 1;

                if ("".equals(temp)) {
                    continue;
                }

                if (fieldLine == curLine) {
                    // 解析表头
                    String[] values = temp.split("\t");
                    if (0 >= values.length) {
                        CommFile.close(br);
                        CommLogD.error("ParseTable fieldCnt <= 0, path:{}", path);
                        return lines;
                    }
                    // 过滤空格
                    fields = new String[values.length];
                    for (int index = 0; index < values.length; index++) {
                        fields[index] = values[index].trim();
                    }
                } else if (curLine >= contentLine) {
                    if (null == fields) {
                        CommFile.close(br);
                        CommLogD.error("ParseTable fieldCnt null(fieldLine: {}), path:{}", fieldLine, path);
                        return lines;
                    }

                    // 解析内容
                    String[] values = temp.split("\t");
                    if (fields.length < values.length) {
                        CommLogD.error("解析表失败 代码字段({}) < 配表字段({}), content:{}, path:{}", fields.length, values.length, temp, path);
                        continue;
                    }

                    // 单行字典
                    Map<String, String> lineValue = new ConcurrentHashMap<>();
                    for (int index = 0; index < fields.length; index++) {
                        if (values.length > index) {
                            lineValue.put(fields[index], values[index].trim());
                        } else {
                            lineValue.put(fields[index], "");
                        }
                    }

                    // 放入总表
                    String key = values[0];
                    if (table.containsKey(key)) {
                        CommLogD.error("键值重复: {}, at line:{}, path:{}", key, curLine, path);
                    }
                    table.put(key, lineValue);
                    lines.add(lineValue);
                }
            } catch (IOException e) {
                CommLogD.error(null, e);
            }
        } while (null != temp);

        CommFile.close(br);

        return lines;
    }

    /**
     * 递归获取指定后缀的文件
     *
     * @param dirPath
     * @param fileList
     * @param postfix
     */
    public static void getFileListByPath(String dirPath, List<File> fileList, String postfix) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            return;
        }

        List<File> subDirFileList = new ArrayList<>();
        List<File> curDirFileList = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                getFileListByPath(file.getAbsolutePath(), subDirFileList, postfix);
            } else {
                if (!postfix.isEmpty() && !file.getName().toLowerCase().endsWith("." + postfix.trim())) {
                    // curDirFileList.add(file);
                    continue;
                } else {
                    curDirFileList.add(file);
                }
            }
        }

        fileList.addAll(subDirFileList);
        fileList.addAll(curDirFileList);
    }

    public static void getLinesFromFile(List<String> methodList, String strFilePath) {
        File file = null;
        BufferedReader br = null;
        String strLine = null;
        try {
            try {
                file = new File(strFilePath);// 文件路径
                br = new BufferedReader(new FileReader(file));
                while ((strLine = br.readLine()) != null) {
                    if (!strLine.trim().isEmpty()) {
                        methodList.add(strLine.trim());
                    }
                }
            } catch (FileNotFoundException e) {
                CommLogD.error(null, e);
            } catch (IOException e) {
                CommLogD.error(null, e);
            } finally {
                if (br != null) {
                    br.close();
                }
            }
        } catch (Exception e) {
            CommLogD.error("getLinesFromFile:" + e.getMessage());
        }

    }

    /**
     * get the byte data of the file
     *
     * @param filePath
     * @return char array of the file content
     */
    public static CharBuffer getFileData(String filePath) {
        CharBuffer data = null;

        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        FileInputStream fin = null;
        InputStreamReader isReader = null;
        BufferedReader bufReader = null;

        try {
            fin = new FileInputStream(file);
            isReader = new InputStreamReader(fin, "UTF-8");
            bufReader = new BufferedReader(isReader);

            data = CharBuffer.allocate((int) file.length());
            int count = bufReader.read(data);
            data.limit(count);
        } catch (Exception e) {
            return null;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
            if (isReader != null) {
                try {
                    isReader.close();
                } catch (IOException e) {
                }
            }
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                }
            }
        }

        return data;
    }

    public static String getTextFromFile(String strFilePath) {
        CharBuffer buf = getFileData(strFilePath);
        if (buf != null) {
            String res = new String(buf.array());
            return res.trim();
        }
        return null;
    }

    public static Set<String> getLineSetsFromFile(String strFilePath) {
        Set<String> rowsets = new HashSet<>();
        List<String> lines = getLinesFromFile(strFilePath);
        for (String line : lines) {
            rowsets.add(line);
        }
        return rowsets;
    }

    public static List<String> getLinesFromFile(String strFilePath) {
        File file = null;
        BufferedReader br = null;
        String strLine = null;
        List<String> rowsets = new LinkedList<>();
        try {
            file = new File(strFilePath);// 文件路径
            br = new BufferedReader(new FileReader(file));
            while ((strLine = br.readLine()) != null) {
                if (!strLine.trim().isEmpty()) {
                    rowsets.add(strLine.trim());
                }
            }
        } catch (FileNotFoundException e) {
            CommLogD.error("getLinesFromFile", e);
        } catch (IOException e) {
            CommLogD.error("getLinesFromFile", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    CommLogD.error("getLinesFromFile", e);
                }
            }
        }
        return rowsets;
    }

    /**
     * 拷贝文件
     *
     * @param from
     * @param to
     * @throws IOException
     */
    public static void Copy(String from, String to) throws IOException {
        RandomAccessFile fromFile = null;
        RandomAccessFile toFile = null;
        try {

            String currentDir = System.getProperty("user.dir");
            CommLogD.info("Current dir using System:" + currentDir);

            fromFile = new RandomAccessFile(from, "r");
            FileChannel fromChannel = fromFile.getChannel();

            toFile = new RandomAccessFile(to, "rw");
            FileChannel toChannel = toFile.getChannel();

            long size = fromChannel.size();

            fromChannel.transferTo(0, size, toChannel);

        } finally {
            if (fromFile != null) {
                fromFile.close();
            }
            if (toFile != null) {
                toFile.close();
            }
        }
    }

    /**
     * 保存内容到路径
     *
     * @param path
     * @param content
     * @throws IOException
     */
    public static void Write(String path, byte[] content) throws IOException {
        Write(path, new String(content));
    }

    public static void Write(String path, String content) throws IOException {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory: " + parent);
        }
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f), java.nio.charset.StandardCharsets.UTF_8)) {
            osw.write(content);
        }
    }

	/**
	 * 获取第一行
	 * 回放专用
	 * @param strFilePath
	 * @return
	 */
	public static String getHandFromFile(String strFilePath) {
	    if (StringUtils.isEmpty(strFilePath)) {
	        return null;
        }
		File file = null;
		BufferedReader br = null;
		String strLine = null;
		try {
			file = new File(strFilePath);// 文件路径
            if (!file.exists()) {
                return null;
            }
			br = new BufferedReader(new FileReader(file));
			while ((strLine = br.readLine()) != null) {
				if (!strLine.trim().isEmpty()) {
					int indexOf = strLine.indexOf("-");
					if (indexOf <= -1) {
						return "";
					}
					return strLine.substring(0, indexOf);
				}
			}
		} catch (FileNotFoundException e) {
			CommLogD.error("getLinesFromFile", e);
		} catch (IOException e) {
			CommLogD.error("getLinesFromFile", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					CommLogD.error("getLinesFromFile", e);
				}
			}
		}
		return "";
	}

	/**
	 * 获取内容
	 * 回放专用
	 * @param strFilePath
	 * @return
	 */
	public static String getContentFromFile(String strFilePath) {
        if (StringUtils.isEmpty(strFilePath)) {
            return null;
        }
		String str = getTextFromFile(strFilePath);
		if(StringUtils.isEmpty(str)) {
			return null;
		}
		int indexOf = str.indexOf("{");
		if (indexOf <= -1) {
			return null;
		}
		return str.substring(indexOf,str.length());
	}

    /**
     * 文件名称列表
     * @param path 路径
     * @return
     */
    public static List<String> FileNameList(String path) {
        File f = new File(path);
        if (!f.isDirectory()) {
            return Lists.newArrayList();
        }
        return Arrays.stream(f.listFiles()).filter(k->k.isDirectory()).map(k->k.list()).flatMap(k-> Arrays.stream(k)).collect(Collectors.toList());
    }
    /**
     * 从包package中获取所有的Class
     *
     * @param
     * @return
     */
    public static List<Class<?>> getClasses(String packageName) {

        //第一个class类的集合
        List<Class<?>> classes = new ArrayList<Class<?>>();
        //是否循环迭代
        boolean recursive = true;
        //获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        //定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            //循环迭代下去
            while (dirs.hasMoreElements()) {
                //获取下一个元素
                URL url = dirs.nextElement();
                //得到协议的名称
                String protocol = url.getProtocol();
                //如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    //获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    //以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    //如果是jar包文件
                    //定义一个JarFile
                    JarFile jar;
                    try {
                        //获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        //从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        //同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            //获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            //如果是以/开头的
                            if (name.charAt(0) == '/') {
                                //获取后面的字符串
                                name = name.substring(1);
                            }
                            //如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                //如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    //获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                //如果可以迭代下去 并且是一个包
                                if ((idx != -1) || recursive) {
                                    //如果是一个.class文件 而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        //去掉后面的".class" 获取真正的类名
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            //添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            CommLogD.error("File operation failed", e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        CommLogD.error("File operation failed", e);
                    }
                }
            }
        } catch (IOException e) {
            CommLogD.error("File operation failed", e);
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, List<Class<?>> classes) {
        //获取此包的目录 建立一个File
        File dir = new File(packagePath);
        //如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        //如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            //自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        //循环所有文件
        for (File file : dirfiles) {
            //如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(),
                        file.getAbsolutePath(),
                        recursive,
                        classes);
            } else {
                //如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    //添加到集合中去
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    CommLogD.error("File operation failed", e);
                }
            }
        }
    }

    public static Class Clazz (String classPath) {
        try {
            return Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            CommLogD.error("File operation failed", e);
        }
        return null;
    }

    /**
     * 跟踪日志
     */
    public static void stackTrace(){
        StringBuilder stringBuilder = new StringBuilder();
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                stringBuilder.append("("+stackElements[i].getClassName()+"---->"+stackElements[i].getFileName()+"--->"+stackElements[i].getLineNumber()+"--->"+stackElements[i].getMethodName()+")"+"\n");
            }
        }
        CommLogD.error(stringBuilder.toString());
    }
}
