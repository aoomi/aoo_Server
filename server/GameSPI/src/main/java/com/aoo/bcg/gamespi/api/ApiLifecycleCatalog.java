package com.aoo.bcg.gamespi.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Version history for interfaces, messages and their public fields. */
public final class ApiLifecycleCatalog {
    public record FieldMetadata(String name, SemanticVersion introducedVersion) {
        public FieldMetadata { if (name == null || name.isBlank() || introducedVersion == null) throw new IllegalArgumentException("incomplete field lifecycle metadata"); }
    }
    public record Contract(ApiOwnershipCatalog.Transport transport, String endpoint,
                           SemanticVersion introducedVersion, Map<String,FieldMetadata> fields) {
        public Contract {
            if (transport == null || endpoint == null || endpoint.isBlank() || introducedVersion == null || fields == null || fields.isEmpty())
                throw new IllegalArgumentException("incomplete API lifecycle metadata");
            fields = Map.copyOf(fields);
            if (fields.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(entry.getValue().name())))
                throw new IllegalArgumentException("field lifecycle key mismatch");
        }
    }
    private final Map<String,Contract> contracts;
    public ApiLifecycleCatalog(List<Contract> contracts) {
        Map<String,Contract> indexed = new LinkedHashMap<>();
        for (Contract contract : contracts) {
            String key = contract.transport() + ":" + contract.endpoint();
            if (indexed.putIfAbsent(key, contract) != null) throw new IllegalArgumentException("duplicate lifecycle contract: " + key);
        }
        this.contracts = Map.copyOf(indexed);
    }
    public Contract require(ApiOwnershipCatalog.Transport transport, String endpoint) {
        Contract exact = contracts.get(transport + ":" + endpoint);
        if (exact != null) return exact;
        return contracts.values().stream().filter(value -> value.transport() == transport && value.endpoint().endsWith(".*")
            && endpoint.startsWith(value.endpoint().substring(0, value.endpoint().length()-1))).findFirst()
            .orElseThrow(() -> new IllegalStateException("API lifecycle metadata missing: " + endpoint));
    }
    public List<Contract> contracts() { return List.copyOf(contracts.values()); }

    public static ApiLifecycleCatalog standard() {
        SemanticVersion v2 = SemanticVersion.parse("2.0.0");
        Map<String,FieldMetadata> http = fields(v2, "requestId", "traceId", "timestamp", "data");
        Map<String,FieldMetadata> wss = fields(v2, "msgId", "requestId", "seq", "roomId", "roundNo", "playVersion", "timestamp", "body");
        var values = new java.util.ArrayList<Contract>();
        for (var owner : ApiOwnershipCatalog.standard().entries())
            values.add(new Contract(owner.transport(), owner.pattern(), v2, owner.transport()==ApiOwnershipCatalog.Transport.HTTP ? http : wss));
        return new ApiLifecycleCatalog(values);
    }
    private static Map<String,FieldMetadata> fields(SemanticVersion version, String... names) {
        Map<String,FieldMetadata> result = new LinkedHashMap<>();
        for (String name : names) result.put(name, new FieldMetadata(name, version));
        return Map.copyOf(result);
    }
}
