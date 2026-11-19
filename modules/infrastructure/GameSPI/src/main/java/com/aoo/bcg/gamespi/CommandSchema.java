package com.aoo.bcg.gamespi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Closed-world payload schema. Unknown and server-controlled client fields are rejected. */
public final class CommandSchema {
    private final Map<String, CommandFieldSpec> fields;

    public CommandSchema(List<CommandFieldSpec> fields) {
        Map<String, CommandFieldSpec> indexed = new LinkedHashMap<>();
        for (CommandFieldSpec field : fields == null ? List.<CommandFieldSpec>of() : fields) {
            if (indexed.putIfAbsent(field.name(), field) != null)
                throw new IllegalArgumentException("duplicate command field: " + field.name());
        }
        this.fields = Map.copyOf(indexed);
    }

    public static CommandSchema of(CommandFieldSpec... fields) {
        return new CommandSchema(fields == null ? List.of() : List.of(fields));
    }

    public void validate(CommandPayload payload) {
        CommandPayload value = payload == null ? CommandPayload.empty() : payload;
        for (String supplied : value.keySet()) {
            CommandFieldSpec field = fields.get(supplied);
            if (field == null) throw new IllegalArgumentException("unknown command field: " + supplied);
            if (field.trust() != CommandFieldTrust.CLIENT_INTENT)
                throw new SecurityException("client supplied server-controlled field: " + supplied);
            field.validate(value.get(supplied));
        }
        fields.values().stream().filter(CommandFieldSpec::required)
                .filter(field -> !value.containsKey(field.name()))
                .findFirst().ifPresent(field -> {
                    throw new IllegalArgumentException("missing command field: " + field.name());
                });
    }

    public Map<String, CommandFieldSpec> fields() { return fields; }
}
