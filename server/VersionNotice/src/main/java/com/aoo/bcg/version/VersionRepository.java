package com.aoo.bcg.version;
import java.time.Instant;import java.util.List;import java.util.Optional;
import static com.aoo.bcg.version.VersionModels.*;
public interface VersionRepository {
    Optional<Release> eligibleRelease(String platform,String channel,int bucket,Instant now);
    Optional<Release> rollback(String platform,String channel,String version);
    List<Notice> notices(String platform,String channel,Instant now);
    Optional<Maintenance> maintenance(String platform,String channel,Instant now);
    Optional<Manifest> manifest(long releaseId);
    List<FeatureFlag> featureFlags(String platform,String channel,int bucket,Instant now);
    Directory directory(String platform,String channel,int bucket,Long preferredServerId,Instant now);
}
