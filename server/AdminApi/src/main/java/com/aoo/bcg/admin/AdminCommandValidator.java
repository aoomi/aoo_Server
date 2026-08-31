package com.aoo.bcg.admin;

import java.util.Map;
import java.util.Set;

public final class AdminCommandValidator {
    private static final Set<String> COMMON = Set.of("requestId", "reason");

    public void validate(String resourceType, String resourceId, Map<String, Object> command) {
        requiredText(command, "requestId", 128);
        requiredText(command, "reason", 500);
        switch (resourceType) {
            case "operation-switches" -> operationSwitch(resourceId, command);
            case "appeals" -> appeal(command);
            case "data-lifecycle" -> lifecycle(resourceId, command);
            default -> throw new IllegalArgumentException("unsupported resource type");
        }
    }

    private void operationSwitch(String gameId, Map<String, Object> command) {
        positiveLong(gameId, "gameId");
        only(command, Set.of("requestId", "reason", "allowCreate", "allowExistingFinish",
                "minimumClientVersion", "routeVersion"));
        requiredBoolean(command, "allowCreate");
        requiredBoolean(command, "allowExistingFinish");
        requiredText(command, "minimumClientVersion", 64);
        requiredText(command, "routeVersion", 64);
    }

    private void appeal(Map<String, Object> command) {
        only(command, Set.of("requestId", "reason", "resolution", "evidenceReference"));
        String resolution = requiredText(command, "resolution", 32);
        if (!Set.of("APPROVED", "REJECTED", "PARTIAL", "NEED_MORE_EVIDENCE").contains(resolution)) {
            throw new IllegalArgumentException("invalid appeal resolution");
        }
        optionalText(command, "evidenceReference", 500);
    }

    private void lifecycle(String dataType, Map<String, Object> command) {
        if (!Set.of("ACCOUNT", "ROOM", "REPLAY", "BILLING", "CHAT", "AUDIT", "SECURITY_EVENT")
                .contains(dataType)) throw new IllegalArgumentException("invalid data type");
        only(command, Set.of("requestId", "reason", "hotDays", "archiveDays",
                "deleteAfterDays", "legalHold", "anonymizeOnDeletion"));
        int hot = requiredInteger(command, "hotDays", 0, 3650);
        int archive = requiredInteger(command, "archiveDays", hot, 36500);
        requiredInteger(command, "deleteAfterDays", archive, 36500);
        requiredBoolean(command, "legalHold");
        requiredBoolean(command, "anonymizeOnDeletion");
    }

    private void only(Map<String, Object> command, Set<String> fields) {
        if (!fields.containsAll(command.keySet())) throw new IllegalArgumentException("unknown command field");
    }

    private String requiredText(Map<String, Object> command, String field, int maxLength) {
        Object value = command.get(field);
        if (!(value instanceof String text) || text.isBlank() || text.length() > maxLength) {
            throw new IllegalArgumentException(field + " invalid");
        }
        return text;
    }

    private void optionalText(Map<String, Object> command, String field, int maxLength) {
        Object value = command.get(field);
        if (value != null && (!(value instanceof String text) || text.length() > maxLength)) {
            throw new IllegalArgumentException(field + " invalid");
        }
    }

    private void requiredBoolean(Map<String, Object> command, String field) {
        if (!(command.get(field) instanceof Boolean)) throw new IllegalArgumentException(field + " invalid");
    }

    private int requiredInteger(Map<String, Object> command, String field, int minimum, int maximum) {
        Object value = command.get(field);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(field + " invalid");
        int result = number.intValue();
        if (result < minimum || result > maximum || number.doubleValue() != result) {
            throw new IllegalArgumentException(field + " invalid");
        }
        return result;
    }

    private long positiveLong(String value, String field) {
        try {
            long result = Long.parseLong(value);
            if (result <= 0) throw new IllegalArgumentException(field + " invalid");
            return result;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(field + " invalid", error);
        }
    }
}
