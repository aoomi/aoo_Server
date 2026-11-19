package com.aoo.bcg.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OwnedLayerPurityTest {
    private static final List<String> CONCRETE_GAMES = List.of(
            "chengdu1001", "scjymj", "xuezhan", "njpdk", "xcpdk", "paodekuai");

    @Test void commonProductionSourcesHaveNoConcreteGameReverseDependencyOrReflection() throws Exception {
        Path source = Path.of(System.getProperty("basedir", "."), "src/main/java");
        List<Path> sourceFiles;
        try (var files = Files.walk(source)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }
        Set<String> modulePackages = sourcePackages(sourceFiles);
        Set<String> dependencyPrefixes = dependencyPrefixes(
                Path.of(System.getProperty("basedir", "."), "pom.xml"));
        List<String> violations = new ArrayList<>();
        for (Path file : sourceFiles) {
            int lineNo = 0;
            for (String line : Files.readAllLines(file)) {
                lineNo++;
                String trimmed = line.strip();
                String lower = line.toLowerCase(Locale.ROOT);
                if (trimmed.startsWith("import ")
                        && !allowedImport(trimmed, modulePackages, dependencyPrefixes))
                    violations.add(file + ":" + lineNo + " reverse import " + trimmed);
                if (CONCRETE_GAMES.stream().anyMatch(lower::contains))
                    violations.add(file + ":" + lineNo + " concrete game reference");
                if (line.contains("setAccessible(") || line.contains("getDeclaredField(")
                        || line.contains("Class.forName("))
                    violations.add(file + ":" + lineNo + " reflective access");
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }

    private static boolean allowedImport(
            String value, Set<String> modulePackages, Set<String> dependencyPrefixes) {
        String imported = value.substring("import ".length()).stripLeading();
        if (imported.startsWith("static ")) imported = imported.substring("static ".length()).stripLeading();

        if (imported.startsWith("java.") || imported.startsWith("javax.")) return true;
        if (imported.startsWith("com.aoo.bcg.")) {
            // 仓内包必须硬保护，不能被 pom 中较宽的第三方前缀绕过分层契约。
            return imported.startsWith("com.aoo.bcg.common.")
                    || imported.startsWith("com.aoo.bcg.gamespi.")
                    || modulePackages.stream().anyMatch(imported::startsWith);
        }
        return modulePackages.stream().anyMatch(imported::startsWith)
                || dependencyPrefixes.stream().anyMatch(imported::startsWith);
    }

    private static Set<String> sourcePackages(List<Path> sourceFiles) throws Exception {
        Set<String> packages = new HashSet<>();
        Pattern packageDeclaration = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
        for (Path file : sourceFiles) {
            var matcher = packageDeclaration.matcher(Files.readString(file));
            if (matcher.find()) packages.add(matcher.group(1) + ".");
        }
        return packages;
    }

    private static Set<String> dependencyPrefixes(Path pom) throws Exception {
        // 第三方允许范围由本模块 pom 派生，使依赖声明成为唯一事实来源。
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        Set<String> prefixes = new HashSet<>();
        var dependencies = document.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String groupId = null;
            String scope = "";
            for (Node child = dependency.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() != Node.ELEMENT_NODE) continue;
                if ("groupId".equals(child.getNodeName())) groupId = child.getTextContent().strip();
                if ("scope".equals(child.getNodeName())) scope = child.getTextContent().strip();
            }
            if (groupId == null || !(scope.isEmpty() || "compile".equals(scope)
                    || "runtime".equals(scope) || "provided".equals(scope))) continue;
            prefixes.add(groupId + ".");
            int lastDot = groupId.lastIndexOf('.');
            String parent = lastDot < 0 ? "" : groupId.substring(0, lastDot);
            if (parent.chars().filter(ch -> ch == '.').count() >= 2) prefixes.add(parent + ".");
        }
        return prefixes;
    }
}
