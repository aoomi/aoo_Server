import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Emits deterministic class-origin evidence and rejects shadowed class resources. */
public final class ClassOriginDiagnostics {
    private ClassOriginDiagnostics() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("at least one class name is required");
        List<String> rows = new ArrayList<>();
        boolean passed = true;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String className : args) {
            Class<?> type = Class.forName(className, false, loader);
            String resourceName = className.replace('.', '/') + ".class";
            List<URL> resources = Collections.list(loader.getResources(resourceName));
            CodeSource source = type.getProtectionDomain().getCodeSource();
            String location = source == null ? "jrt:/" : source.getLocation().toExternalForm();
            String moduleName = type.getModule().isNamed() ? type.getModule().getName() : "unnamed";
            boolean unique = resources.size() == 1;
            passed &= unique;
            rows.add("{\"className\":\"" + escape(className) + "\",\"moduleName\":\"" + escape(moduleName)
                    + "\",\"codeSource\":\"" + escape(location) + "\",\"resourceCount\":" + resources.size()
                    + ",\"resources\":[" + resources.stream().map(url -> "\"" + escape(url.toExternalForm()) + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]}");
        }
        System.out.println("{\"passed\":" + passed + ",\"classes\":[" + String.join(",", rows) + "]}");
        if (!passed) System.exit(1);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
