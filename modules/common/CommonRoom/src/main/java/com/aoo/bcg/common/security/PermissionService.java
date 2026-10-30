package com.aoo.bcg.common.security;

public interface PermissionService {
    Decision authorize(long operatorId, String permission, Resource resource);
    record Resource(String type, long id, String tenantId) {}
    record Decision(boolean allowed, String reasonCode) {
        public void requireAllowed() { if (!allowed) throw new SecurityException(reasonCode); }
    }
}
