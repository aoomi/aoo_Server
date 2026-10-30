package com.aoo.bcg.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CanonicalClubCreatePolicyTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ObjectNode action(boolean eligibility, long minimum, boolean charge, long cost) throws Exception {
        ObjectNode n = JSON.createObjectNode();
        n.put("currency", "ROOM_CARD"); n.put("eligibilityEnabled", eligibility);
        n.put("minimumRoomCardBalance", minimum); n.put("chargeEnabled", charge);
        n.put("chargeRoomCardAmount", cost); n.put("policyVersion", 1);
        sign(n); return n;
    }
    private void sign(ObjectNode n) throws Exception {
        String payload = String.join("\n", "schemaVersion=1", "action=CLUB_CREATE", "currency="+n.get("currency").asText(),
                "eligibilityEnabled="+n.get("eligibilityEnabled").asText(), "minimumRoomCardBalance="+n.get("minimumRoomCardBalance").asText(),
                "chargeEnabled="+n.get("chargeEnabled").asText(), "chargeRoomCardAmount="+n.get("chargeRoomCardAmount").asText(),
                "policyVersion="+n.get("policyVersion").asText());
        n.put("contentHash", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))));
    }
    private CanonicalPricingPolicy parse(ObjectNode n) throws Exception {
        var m = CanonicalPricingPolicy.class.getDeclaredMethod("parse", com.fasterxml.jackson.databind.JsonNode.class);
        m.setAccessible(true);
        try { return (CanonicalPricingPolicy)m.invoke(null, n); }
        catch (InvocationTargetException e) { throw (RuntimeException)e.getCause(); }
    }
    private ObjectNode published() throws Exception {
        try(var in = CanonicalPricingPolicy.class.getResourceAsStream(CanonicalPricingPolicy.RESOURCE)) {
            return (ObjectNode)JSON.readTree(in);
        }
    }
    @Test void publishedValuesAndGamePolicyRemainValid() throws Exception {
        var policy = CanonicalPricingPolicy.load(); var club = policy.requireClubCreate();
        assertEquals("ROOM_CARD", club.currency()); assertEquals(500, club.minimumRoomCardBalance());
        assertEquals(100, club.chargeRoomCardAmount()); assertTrue(club.eligibilityEnabled()); assertTrue(club.chargeEnabled());
        var root=published(); var game=root.get("policies").get(0);
        assertTrue(policy.forIdentity(game.get("gameId").longValue(),game.get("playVersion").asText(),game.get("regionCode").asText()).isPresent());
    }
    @Test void independentFlagsAndZeroValues() throws Exception {
        for(boolean eligibility : new boolean[]{false,true}) for(boolean charge : new boolean[]{false,true}) {
            var root=published(); root.set("clubCreate",action(eligibility,0,charge,0));
            var c=parse(root).requireClubCreate(); assertEquals(eligibility,c.eligibilityEnabled());
            assertEquals(charge,c.chargeEnabled()); assertEquals(0,c.minimumRoomCardBalance()); assertEquals(0,c.chargeRoomCardAmount());
        }
    }
    @Test void exactLongBoundaryAccepted() throws Exception {
        var root=published(); root.set("clubCreate",action(true,Long.MAX_VALUE,true,Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE,parse(root).requireClubCreate().minimumRoomCardBalance());
    }
    @Test void missingActionFailsOnlyRequiredCreatePort() throws Exception {
        var root=published(); root.remove("clubCreate"); var policy=parse(root);
        assertThrows(IllegalStateException.class, policy::requireClubCreate);
    }
    @Test void invalidTypesRangesAndVersionRejected() throws Exception {
        String[] bad={"{\"eligibilityEnabled\":\"true\"}","{\"minimumRoomCardBalance\":-1}",
            "{\"minimumRoomCardBalance\":9223372036854775808}","{\"chargeRoomCardAmount\":1.5}",
            "{\"policyVersion\":0}","{\"policyVersion\":18446744073709551617}","{\"currency\":\"CRYSTAL\"}"};
        for(String patch:bad) { var root=published(); var n=action(true,500,true,100); n.setAll((ObjectNode)JSON.readTree(patch)); root.set("clubCreate",n);
            assertThrows(IllegalStateException.class,()->parse(root)); }
        var root=published(); root.putNull("clubCreate"); assertThrows(IllegalStateException.class,()->parse(root));
    }
    @Test void tamperingHashOrContentRejected() throws Exception {
        var root=published(); ((ObjectNode)root.get("clubCreate")).put("chargeRoomCardAmount",101);
        assertThrows(IllegalStateException.class,()->parse(root));
        var root2=published(); ((ObjectNode)root2.get("clubCreate")).put("contentHash","0".repeat(64));
        assertThrows(IllegalStateException.class,()->parse(root2));
    }
}
