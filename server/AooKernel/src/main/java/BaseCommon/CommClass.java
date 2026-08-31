/*
 * Decompiled with CFR 0.152.
 */
package BaseCommon;

import BaseCommon.CommLog;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CommClass {
    private static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    public static void setClassLoader(ClassLoader classLoader) {
        CommClass.classLoader = classLoader;
    }

    public static Set<Class<?>> getClasses(String pack) {
        LinkedHashSet classes = new LinkedHashSet();
        boolean recursive = true;
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        try {
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            block14: while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if (protocol == null) continue;
                switch (protocol) {
                    case "file": {
                        String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                        CommClass.findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                        break;
                    }
                    case "jar": {
                        try {
                            JarFile jar = ((JarURLConnection)url.openConnection()).getJarFile();
                            Enumeration<JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String name = entry.getName();
                                if (name.charAt(0) == '/') {
                                    name = name.substring(1);
                                }
                                if (!name.startsWith(packageDirName)) continue;
                                int idx = name.lastIndexOf(47);
                                if (idx != -1) {
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                if (idx == -1 && !recursive || !name.endsWith(".class") || entry.isDirectory()) continue;
                                String className = name.substring(packageName.length() + 1, name.length() - 6);
                                try {
                                    classes.add(classLoader.loadClass(String.valueOf(packageName) + '.' + className));
                                }
                                catch (ClassNotFoundException e) {
                                    CommLog.error(CommClass.class.getName(), e);
                                }
                            }
                            continue block14;
                        }
                        catch (IOException e) {
                            CommLog.error(CommClass.class.getName(), e);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            CommLog.error(CommClass.class.getName(), e);
        }
        return classes;
    }

    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File[] dirfiles;
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] fileArray = dirfiles = dir.listFiles(new FileFilter(){

            @Override
            public boolean accept(File file) {
                return recursive && file.isDirectory() || file.getName().endsWith(".class");
            }
        });
        int n = dirfiles.length;
        int n2 = 0;
        while (n2 < n) {
            File file = fileArray[n2];
            if (file.isDirectory()) {
                CommClass.findAndAddClassesInPackageByFile(String.valueOf(packageName) + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(classLoader.loadClass(String.valueOf(packageName) + '.' + className));
                }
                catch (ClassNotFoundException e) {
                    CommLog.error(CommClass.class.getName(), e);
                }
            }
            ++n2;
        }
    }

    public static List<Class<?>> getAllAssignedClass(Class<?> cls) throws IOException, ClassNotFoundException {
        ArrayList classes = new ArrayList();
        for (Class<?> c : CommClass.getClasses(cls)) {
            if (!cls.isAssignableFrom(c) || cls.equals(c)) continue;
            classes.add(c);
        }
        return classes;
    }

    public static List<Class<?>> getClasses(Class<?> cls) throws IOException, ClassNotFoundException {
        String pk = cls.getPackage().getName();
        String path = pk.replace('.', '/');
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(path);
        return CommClass.getClasses(new File(url.getFile()), pk);
    }

    private static List<Class<?>> getClasses(File dir, String pk) throws ClassNotFoundException {
        ArrayList classes = new ArrayList();
        if (!dir.exists()) {
            return classes;
        }
        File[] fileArray = dir.listFiles();
        int n = fileArray.length;
        int n2 = 0;
        while (n2 < n) {
            String name;
            File f = fileArray[n2];
            if (f.isDirectory()) {
                classes.addAll(CommClass.getClasses(f, String.valueOf(pk) + "." + f.getName()));
            }
            if ((name = f.getName()).endsWith(".class")) {
                classes.add(CommClass.forName(String.valueOf(pk) + "." + name.substring(0, name.length() - 6)));
            }
            ++n2;
        }
        return classes;
    }

    public static List<Class> getAllClassByInterface(Class c) {
        ArrayList<Class> returnClassList = new ArrayList<Class>();
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
            String packageName = c.getPackage().getName();
            Set<Class<?>> allClass = CommClass.getClasses(packageName);
            for (Class<?> cs : allClass) {
                if (!c.isAssignableFrom(cs) || c.equals(cs)) continue;
                returnClassList.add(cs);
            }
        }
        return returnClassList;
    }

    public static List<Class<?>> getAllClassByInterface(Class<?> c, String packageName) {
        ArrayList returnClassList = new ArrayList();
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
            Set<Class<?>> allClass = CommClass.getClasses(packageName);
            for (Class<?> cs : allClass) {
                if (!c.isAssignableFrom(cs) || Modifier.isAbstract(cs.getModifiers()) || c.equals(cs)) continue;
                returnClassList.add(cs);
            }
        }
        return returnClassList;
    }

    public static String printClassInfo(Object object) {
        Field[] fields;
        StringBuilder sBuilder = new StringBuilder();
        String ent = System.lineSeparator();
        sBuilder.append("output:").append(object.getClass().getSimpleName()).append(ent);
        Field[] fieldArray = fields = object.getClass().getDeclaredFields();
        int n = fields.length;
        int n2 = 0;
        while (n2 < n) {
            Field field = fieldArray[n2];
            try {
                boolean accessFlag = field.isAccessible();
                field.setAccessible(true);
                String varName = field.getName();
                Object varValue = field.get(object);
                sBuilder.append(String.format("(%s)%s = %s", field.getType().getSimpleName(), varName, varValue)).append(ent);
                field.setAccessible(accessFlag);
            }
            catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
                CommLog.error(CommClass.class.getName(), e);
            }
            ++n2;
        }
        return sBuilder.toString();
    }

    public static String getClassPropertyInfos(Object object) {
        Field[] fields;
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("[");
        Field[] fieldArray = fields = object.getClass().getDeclaredFields();
        int n = fields.length;
        int n2 = 0;
        while (n2 < n) {
            Field field = fieldArray[n2];
            try {
                boolean accessFlag = field.isAccessible();
                field.setAccessible(true);
                String varName = field.getName();
                Object varValue = field.get(object);
                Class<?> type = field.getType();
                if (type.isAssignableFrom(Collection.class.getClass())) {
                    Collection lst = (Collection)varValue;
                    for (Object objInlist : lst) {
                        sBuilder.append(CommClass.getClassPropertyInfos(objInlist));
                    }
                } else {
                    sBuilder.append(String.format("%s:%s,", varName, varValue));
                }
                field.setAccessible(accessFlag);
            }
            catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
                CommLog.error(CommClass.class.getName(), e);
            }
            ++n2;
        }
        sBuilder.append("],");
        return sBuilder.toString();
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }
}

