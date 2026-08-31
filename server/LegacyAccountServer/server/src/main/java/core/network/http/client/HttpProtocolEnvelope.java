package core.network.http.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public final class HttpProtocolEnvelope {
    private static final Gson GSON = new Gson();
    public String protocolVersion;
    public String msgId;
    public String kind;
    public String requestId;
    public long seq;
    public String traceId;
    public long timestamp;
    public Integer code;
    public String message;
    @JsonIgnore
    public JsonElement body;

    @JsonProperty("body")
    public Object bodyForJson() {
        return body == null ? null : GSON.fromJson(body, Object.class);
    }
}
