package com.ddm.server.protocol.v2;

import com.aoo.bcg.gamespi.protocol.GeneratedProtocolIds;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/** Application-protocol bridge used while legacy handlers are incrementally replaced. */
public final class ProtocolV2Bridge {
    public static final String DISPATCH_EVENT = "protocol.v2.dispatch";
    private static final Gson GSON = new Gson();
    /** 跑得快（poker:pao-de-kuai）全部地区业务编码的写协议 id；CX 属另一玩法族，不在其中。 */
    private static final Set<String> PDK_DISPATCH_IDS = Set.of(
            GeneratedProtocolIds.POKER_CHENGDU2001_DISPATCH, GeneratedProtocolIds.POKER_NEIJIANG2002_DISPATCH,
            GeneratedProtocolIds.POKER_LIANGSHAN2003_DISPATCH, GeneratedProtocolIds.POKER_MIANNING2004_DISPATCH,
            GeneratedProtocolIds.POKER_ANYUE2005_DISPATCH, GeneratedProtocolIds.POKER_PANZHIHUA2006_DISPATCH,
            GeneratedProtocolIds.POKER_WANNIAN2007_DISPATCH, GeneratedProtocolIds.POKER_LEPING2008_DISPATCH,
            GeneratedProtocolIds.POKER_BOZHOU2009_DISPATCH, GeneratedProtocolIds.POKER_YUGAN2010_DISPATCH,
            GeneratedProtocolIds.POKER_JIAN2011_DISPATCH, GeneratedProtocolIds.POKER_GUSHI2012_DISPATCH,
            GeneratedProtocolIds.POKER_XIAOGAN2013_DISPATCH, GeneratedProtocolIds.POKER_NINGDU2014_DISPATCH,
            GeneratedProtocolIds.POKER_HUAIHUA2015_DISPATCH, GeneratedProtocolIds.POKER_XINXIAN2016_DISPATCH,
            GeneratedProtocolIds.POKER_JINGDEZHEN2017_DISPATCH, GeneratedProtocolIds.POKER_JIANGSU2018_DISPATCH,
            GeneratedProtocolIds.POKER_HENAN2019_DISPATCH, GeneratedProtocolIds.POKER_TAIZHOU2020_DISPATCH,
            GeneratedProtocolIds.POKER_HUAIBIN2021_DISPATCH, GeneratedProtocolIds.POKER_HUANGCHUAN2022_DISPATCH,
            GeneratedProtocolIds.POKER_POYANG2023_DISPATCH, GeneratedProtocolIds.POKER_JINTAN2024_DISPATCH,
            GeneratedProtocolIds.POKER_GAOAN2025_DISPATCH, GeneratedProtocolIds.POKER_YIXING2026_DISPATCH,
            GeneratedProtocolIds.POKER_XUANCHENG2027_DISPATCH, GeneratedProtocolIds.POKER_FUZHOU2028_DISPATCH);

    private ProtocolV2Bridge() {}

    public static void dispatch(WebSocketRequest request, String json) throws Exception {
        ProtocolEnvelope envelope = GSON.fromJson(json, ProtocolEnvelope.class);
        ProtocolValidator.validate(envelope);
        GeneratedProtocolIds.Definition definition = GeneratedProtocolIds.DEFINITIONS.get(envelope.msgId);
        if (definition == null || !"WSS".equals(definition.transport())
                || !"client_to_server".equals(definition.direction())
                || !definition.kind().equals(envelope.kind)) {
            throw new IllegalArgumentException("Message is not an allowed WSS client request: " + envelope.msgId);
        }
        ProtocolMigrationTelemetry.recordV2(envelope.msgId);
        JsonObject bodyObject = GSON.toJsonTree(envelope.body).getAsJsonObject();
        JsonObject payload = bodyObject;
        long requestedAccountId = payload.has("accountID") ? payload.get("accountID").getAsLong() : 0L;
        if (!request.getSession().isProtocolTicketValidated()) {
            long ticketAccountId = WsTicketVerifier.verifyAndConsume(envelope.wsTicket);
            long existingAccountId = request.getSession().getAccountID();
            if (existingAccountId > 0L && existingAccountId != ticketAccountId) throw new IllegalArgumentException("Session account mismatch");
            if (requestedAccountId > 0L && requestedAccountId != ticketAccountId) throw new IllegalArgumentException("wsTicket account mismatch");
            request.getSession().bindProtocolAccount(ticketAccountId);
        }
        long accountId = request.getSession().getProtocolAuthenticatedAccountId();
        if (requestedAccountId > 0L && requestedAccountId != accountId) throw new IllegalArgumentException("Request account mismatch");
        int scopeRound=envelope.roundNo==null?0:envelope.roundNo;
        // Authoritative commands have one durable idempotency owner in Gateway/JDBC.
        // A transport-local Redis reservation cannot answer an acknowledgement-lost
        // database commit and previously prevented the Gateway outcome query path.
        request.getSession().setProtocolV2(true);
        try {
            request.getSession().acceptProtocolV2Request(envelope.requestId, envelope.seq, envelope.timestamp);
            if (envelope.roomId == null || envelope.roomId.isBlank() || envelope.roundNo == null
                    || envelope.playVersion == null || envelope.playVersion.isBlank()) {
                throw new IllegalArgumentException("authoritative game request requires roomId, roundNo and playVersion");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> commandBody = GSON.fromJson(payload, Map.class);
            String authorityMessageId = authorityMessageId(envelope.msgId, commandBody);
            if (envelope.msgId.endsWith(".dispatch")) commandBody.remove("command");
            Object result = ProtocolV2AuthorityRuntime.dispatch(new ProtocolV2AuthorityRuntime.Command(
                    accountId, authorityMessageId, envelope.requestId, envelope.seq, envelope.roomId,
                    envelope.roundNo, envelope.playVersion, envelope.timestamp, commandBody))
                    .orElseThrow(() -> new IllegalStateException("authoritative game runtime is not installed"));
            ProtocolEnvelope response = response(envelope, result);
            request.response(response);
        } catch (Exception exception) {
            if(String.valueOf(exception.getMessage()).contains("REQUEST_OUTCOME_UNKNOWN")){
                request.response(errorResponse(envelope,ErrorCode.Request_OutcomeUnknown.value(),"REQUEST_OUTCOME_UNKNOWN"));
                return;
            }
            throw exception;
        }
    }

    private static String authorityMessageId(String dispatchId, Map<String,Object> body) {
        if (dispatchId.startsWith("room.")) return dispatchId;
        Object raw = body.get("command");
        if (!(raw instanceof String command) || command.isBlank())
            throw new IllegalArgumentException("authoritative dispatch requires command");
        command = command.trim().toLowerCase(java.util.Locale.ROOT);
        Set<String> common = Set.of("join", "state", "leave", "ready", "unready", "settings", "quick_text",
                "voice", "magic_expression", "invite", "dissolve_apply", "dissolve_vote", "trustee",
                "heartbeat", "reconnect", "shuffle", "kick");
        if (common.contains(command)) return "common.room." + command + "_req";
        if (PDK_DISPATCH_IDS.contains(dispatchId)) {
            if (!Set.of("start","pass","hint","play","play_cards").contains(command))
                throw new IllegalArgumentException("unsupported PDK command");
            return dispatchId.substring(0,dispatchId.length()-"dispatch".length()) + command + "_req";
        }
        if (GeneratedProtocolIds.MAHJONG_XUEZHAN_DISPATCH.equals(dispatchId)) {
            if (!Set.of("start","draw","play","chi","peng","gang","hu","pass").contains(command))
                throw new IllegalArgumentException("unsupported Xuezhan command");
            return "mahjong.xuezhan." + command + "_req";
        }
        // Catalog-routed Mahjong profiles all use one authoritative session
        // contract. Keep the registered dispatch identity intact so the
        // profile handler can validate the action against its own rule set;
        // no profile-specific bridge or CommonRoom installation is needed.
        if (dispatchId.startsWith("mahjong.") && dispatchId.endsWith(".dispatch")) {
            if (!Set.of("state", "join", "ready", "unready", "start", "exchange", "dingque",
                    "draw", "discard", "chi", "peng", "gang", "hu", "pass", "reconnect",
                    "continue", "choice", "timeout", "zhangzhuang", "cbzmjzhangzhuang",
                    "cbzmjgetroominfo", "cbzmjenterroom", "cbzmjreadyroom", "cbzmjstartgame",
                    "cbzmjoutcard", "baoting").contains(command))
                throw new IllegalArgumentException("unsupported Mahjong command");
            return dispatchId;
        }
        if (GeneratedProtocolIds.LONGCARD_AYDSS_DISPATCH.equals(dispatchId)) {
            if (!Set.of("join","state","ready","unready","start","operation").contains(command))
                throw new IllegalArgumentException("unsupported AYDSS command");
            return "longcard.aydss.dispatch";
        }
        if (GeneratedProtocolIds.LONGCARD_AYCP_DISPATCH.equals(dispatchId)) {
            if (!Set.of("join","state","ready","unready","start","operation","piao",
                    "caycpgetroominfo","caycpreadyroom","caycpunreadyroom","caycpstartgame",
                    "caycpopcard","caycpoppiao","caycppiaohua").contains(command))
                throw new IllegalArgumentException("unsupported AYCP command");
            return "longcard.aycp.dispatch";
        }
        throw new IllegalArgumentException("dispatch does not support authoritative commands: " + dispatchId);
    }

    private static ProtocolEnvelope response(ProtocolEnvelope inbound, Object body) {
        ProtocolEnvelope value = new ProtocolEnvelope();
        value.msgId = inbound.msgId;
        value.kind = "resp";
        value.requestId = inbound.requestId;
        value.seq = inbound.seq;
        value.timestamp = System.currentTimeMillis();
        value.traceId = inbound.traceId;
        value.roomId = inbound.roomId;
        value.roundNo = inbound.roundNo;
        value.playVersion = inbound.playVersion;
        value.code = 0;
        value.message = "success";
        value.body = body == null ? new Object() : body;
        return value;
    }

    private static ProtocolEnvelope errorResponse(ProtocolEnvelope inbound,int code,String message){
        ProtocolEnvelope value=response(inbound,Map.of("errorCode",message,"retryable",false));
        value.code=code;value.message=message;return value;
    }
}
