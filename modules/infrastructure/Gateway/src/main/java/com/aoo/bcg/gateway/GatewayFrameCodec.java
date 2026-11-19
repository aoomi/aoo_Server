package com.aoo.bcg.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Bounded UTF-8 JSON codec. Compression is opt-in and decompression is expansion-limited. */
public final class GatewayFrameCodec {
    public static final int MAX_WIRE_BYTES = 256 * 1024;
    public static final int MAX_EXPANDED_BYTES = 1024 * 1024;
    private final ObjectMapper json = new ObjectMapper();
    public byte[] encode(Map<String,Object> frame, boolean gzip) {
        ProtocolValuePolicy.validate(frame);
        try {
            byte[] raw = json.writeValueAsBytes(frame);
            if (raw.length > MAX_EXPANDED_BYTES) throw new IllegalArgumentException("expanded frame too large");
            if (!gzip) { requireWire(raw); return raw; }
            var out = new ByteArrayOutputStream(); try (var zip = new GZIPOutputStream(out)) { zip.write(raw); }
            byte[] encoded = out.toByteArray(); requireWire(encoded); return encoded;
        } catch (IOException error) { throw new IllegalArgumentException("cannot encode frame", error); }
    }
    @SuppressWarnings("unchecked") public Map<String,Object> decode(byte[] wire, boolean gzip) {
        requireWire(wire);
        try {
            var out = new ByteArrayOutputStream();
            try (InputStream input = gzip ? new GZIPInputStream(new ByteArrayInputStream(wire)) : new ByteArrayInputStream(wire)) {
                byte[] buffer = new byte[8192]; int read;
                while ((read = input.read(buffer)) >= 0) { if (out.size() + read > MAX_EXPANDED_BYTES) throw new IllegalArgumentException("expanded frame too large"); out.write(buffer, 0, read); }
            }
            Object decoded=json.readValue(out.toByteArray(),Object.class);if(!(decoded instanceof Map<?,?> raw))throw new IllegalArgumentException("JSON frame root must be an object");
            Map<String,Object> value=(Map<String,Object>)raw; ProtocolValuePolicy.validate(value); return value;
        } catch (JsonProcessingException error) { throw new IllegalArgumentException("malformed UTF-8 JSON frame", error); }
        catch (IOException error) { throw new IllegalArgumentException("cannot decode frame", error); }
    }
    private static void requireWire(byte[] wire) { if (wire == null || wire.length == 0 || wire.length > MAX_WIRE_BYTES) throw new IllegalArgumentException("wire frame size rejected"); }
}
