package com.aoo.bcg.common.trustee;

import java.util.Map;

public record PlayerIntent(String operation, Map<String, Object> arguments) {
    public PlayerIntent { arguments = Map.copyOf(arguments == null ? Map.of() : arguments); }
}
