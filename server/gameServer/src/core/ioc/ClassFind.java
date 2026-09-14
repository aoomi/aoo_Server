//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package core.ioc;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassFind {
    public ClassFind() {
    }

    public static Set<Class<?>> getClasses() {
        return getClasses("");
    }

    public static Set<Class<?>> getClasses(String pack) {
        Set<Class<?>> classes = new LinkedHashSet();
        boolean recursive = true;
        String packageName = pack;
        String packageDirName = pack.replace('.', '/');

        try {
            Enumeration dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);

            for (int scanPass = 0; scanPass < 1; scanPass++) {
                label65:
                while (dirs.hasMoreElements()) {
                    URL url = (URL) dirs.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                        findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                    } else if ("jar".equals(protocol)) {
                        try {
                            JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                            Enumeration entries = jar.entries();

                            while (entries.hasMoreElements()) {
                                JarEntry entry;
                                String name;
                                int idx;
                                do {
                                    do {
                                        if (!entries.hasMoreElements()) {
                                            continue label65;
                                        }

                                        entry = (JarEntry) entries.nextElement();
                                        name = entry.getName();
                                        if (name.charAt(0) == '/') {
                                            name = name.substring(1);
                                        }
                                    } while (!name.startsWith(packageDirName));

                                    idx = name.lastIndexOf(47);
                                    if (idx != -1) {
                                        packageName = name.substring(0, idx).replace('/', '.');
                                    }
                                } while (idx == -1 && !recursive);

                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    String className = name.substring(packageName.length() + 1, name.length() - 6);

                                    try {
                                        classes.add(Class.forName(packageName + '.' + className));
                                    } catch (ClassNotFoundException var15) {
                                        System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", var15);
                                    }
                                }
                            }
                        } catch (IOException var16) {
                            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", var16);
                        }
                    }
                }

                if (classes.isEmpty()) {
                    findAndAddClassesFromClassPath(packageName, packageDirName, recursive, classes);
                }
                return classes;
            }
        } catch (IOException var17) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", var17);
            return classes;
        }
        return classes;
    }

    private static void findAndAddClassesFromClassPath(String packageName, String packageDirName,
                                                       boolean recursive, Set<Class<?>> classes) {
        String[] entries = System.getProperty("java.class.path", "").split(File.pathSeparator);
        for (String entryPath : entries) {
            File entry = new File(entryPath);
            if (entry.isDirectory()) {
                File packageDir = new File(entry, packageDirName);
                findAndAddClassesInPackageByFile(packageName, packageDir.getAbsolutePath(), recursive, classes);
                continue;
            }
            if (!entry.isFile() || !entry.getName().endsWith(".jar")) {
                continue;
            }
            try (JarFile jar = new JarFile(entry)) {
                Enumeration<JarEntry> jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement();
                    String name = jarEntry.getName();
                    if (jarEntry.isDirectory() || !name.startsWith(packageDirName + "/")
                            || !name.endsWith(".class")) {
                        continue;
                    }
                    String relativeName = name.substring(packageDirName.length() + 1);
                    if (!recursive && relativeName.indexOf('/') >= 0) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(className));
                    } catch (Throwable ignored) {
                        // Optional classes may depend on platform-specific libraries.
                    }
                }
            } catch (IOException ignored) {
                // Ignore unreadable classpath entries and continue scanning.
            }
        }
    }

    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        if (packageName.length() > 0) {
            packageName = packageName + ".";
        }

        File dir = new File(packagePath);
        if (dir.exists() && dir.isDirectory()) {
            File[] dirfiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return recursive && file.isDirectory() || file.getName().endsWith(".class");
                }
            });
            File[] var9 = dirfiles;
            int var8 = dirfiles.length;

            for (int var7 = 0; var7 < var8; ++var7) {
                File file = var9[var7];
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(packageName + file.getName(), file.getAbsolutePath(), recursive, classes);
                } else {
                    String className = file.getName().substring(0, file.getName().length() - 6);

                    try {
                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + className));
                    } catch (ClassNotFoundException var12) {
                        System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", var12);
                    }
                }
            }

        }
    }
}
