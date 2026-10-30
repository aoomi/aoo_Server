package com.aoo.bcg.version;

import java.time.Instant;
import java.util.List;

public final class VersionModels {
    private VersionModels() {}
    public record Request(String platform,String channel,String currentVersion,String resourceVersion,String deviceId,Long preferredServerId) {}
    public record Release(long id,String version,String minimumVersion,boolean forceUpdate,int rolloutPercent,String rollbackVersion,String downloadUrl) {}
    public record Notice(long id,String title,String body,String severity,Instant startsAt,Instant endsAt) {}
    public record Maintenance(long id,Instant startsAt,Instant endsAt,String message,boolean loginBlocked) {}
    public record Asset(String path,long sizeBytes,String sha256) {}
    /** Signature is supplied by the release pipeline and verified by clients; this service owns no signing key. */
    public record Manifest(long id,String version,String contentSha256,String signatureAlgorithm,String keyId,String signature,List<Asset> assets) {}
    public record FeatureFlag(String key,boolean enabled,String value) {}
    public record ServerNode(long id,String code,String name,String region,String endpoint,String state,int weight,Long migrateToServerId,String message) {}
    public record Directory(long revision,List<ServerNode> servers,Long selectedServerId) {}
    public record Decision(String status,boolean forceUpdate,String minimumVersion,Release release,Manifest manifest,List<Notice> notices,Maintenance maintenance,List<FeatureFlag> featureFlags,Directory directory) {}
}
