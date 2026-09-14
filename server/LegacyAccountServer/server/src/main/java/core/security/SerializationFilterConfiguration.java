package core.security;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.ObjectInputFilter;
import java.lang.reflect.Proxy;
import java.util.Set;

@Component
public final class SerializationFilterConfiguration {
    private static final Set<String> DENIED_PREFIXES = Set.of(
            "java.rmi.", "javax.management.", "com.sun.jndi.", "org.apache.commons.collections.functors.",
            "org.codehaus.groovy.runtime.", "com.sun.org.apache.xalan.internal.xsltc.trax."
    );

    @PostConstruct
    public void install() {
        if (ObjectInputFilter.Config.getSerialFilter() != null) {
            return;
        }
        ObjectInputFilter.Config.setSerialFilter(info -> {
            if (info.depth() > 64 || info.references() > 100_000 || info.streamBytes() > 16 * 1024 * 1024) {
                return ObjectInputFilter.Status.REJECTED;
            }
            Class<?> type = info.serialClass();
            if (type == null) {
                return ObjectInputFilter.Status.UNDECIDED;
            }
            if (Proxy.isProxyClass(type)) {
                return ObjectInputFilter.Status.REJECTED;
            }
            String name = type.getName();
            return DENIED_PREFIXES.stream().anyMatch(name::startsWith)
                    ? ObjectInputFilter.Status.REJECTED
                    : ObjectInputFilter.Status.UNDECIDED;
        });
    }
}
