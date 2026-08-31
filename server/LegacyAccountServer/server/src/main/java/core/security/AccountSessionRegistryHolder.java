package core.security;

import java.util.Objects;

public final class AccountSessionRegistryHolder {
    private static volatile AccountSessionRegistry registry;
    private AccountSessionRegistryHolder() {}
    public static void install(AccountSessionRegistry value) { registry = Objects.requireNonNull(value); }
    public static AccountSessionRegistry required() {
        AccountSessionRegistry value = registry;
        if (value == null) throw new IllegalStateException("AccountSessionRegistry is not installed");
        return value;
    }
}
