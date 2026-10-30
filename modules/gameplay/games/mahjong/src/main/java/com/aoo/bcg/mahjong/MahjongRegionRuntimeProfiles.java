package com.aoo.bcg.mahjong;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Loads the checked-in 364-row source classification exactly once. */
final class MahjongRegionRuntimeProfiles {
    private static final String RESOURCE = "/mahjong-family-region-config.tsv";
    private static final Map<String,MahjongRegionRuntimeProfile> BY_CODE = load();
    private MahjongRegionRuntimeProfiles() {}

    static Optional<MahjongRegionRuntimeProfile> find(String code) { return Optional.ofNullable(BY_CODE.get(code)); }
    static Collection<MahjongRegionRuntimeProfile> all() { return BY_CODE.values(); }

    private static Map<String,MahjongRegionRuntimeProfile> load() {
        LinkedHashMap<String,MahjongRegionRuntimeProfile> out = new LinkedHashMap<>();
        try (InputStream input = MahjongRegionRuntimeProfiles.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null || !header.startsWith("gameId\tcode\tfamily\t")) throw new IllegalStateException("invalid Mahjong profile header");
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] v = line.split("\t", -1);
                    if (v.length != 11) throw new IllegalStateException("invalid Mahjong profile columns: " + line);
                    MahjongRegionRuntimeProfile profile = new MahjongRegionRuntimeProfile(Integer.parseInt(v[0]),v[1],v[2],v[3],v[4],
                            csv(v[5]),csv(v[6]),v[7],v[8],csv(v[9]),v[10]);
                    if (out.putIfAbsent(profile.code(),profile) != null) throw new IllegalStateException("duplicate Mahjong profile " + profile.code());
                }
            }
        } catch (IOException error) { throw new IllegalStateException("cannot read Mahjong profiles",error); }
        if (out.size() != 364) throw new IllegalStateException("Mahjong profile count=" + out.size() + ", expected=364");
        return Collections.unmodifiableMap(out);
    }
    private static Set<String> csv(String value) {
        if (value == null || value.isBlank() || "none".equals(value)) return Set.of();
        return Set.copyOf(Arrays.stream(value.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());
    }
}
