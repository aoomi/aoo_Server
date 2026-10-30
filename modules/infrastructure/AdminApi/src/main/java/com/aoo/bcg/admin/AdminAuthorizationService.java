package com.aoo.bcg.admin;

@FunctionalInterface
public interface AdminAuthorizationService {
    boolean allowed(long operatorId, String permission);

    /** Data scope uses the same deny-by-default grant source as endpoint permissions. */
    default boolean allowed(long operatorId, String permission, String targetType, String targetId) {
        if (!allowed(operatorId, permission)) return false;
        return allowed(operatorId, "scope.GLOBAL.*")
                || allowed(operatorId, "scope." + targetType + ".*")
                || allowed(operatorId, "scope." + targetType + "." + targetId);
    }
}
