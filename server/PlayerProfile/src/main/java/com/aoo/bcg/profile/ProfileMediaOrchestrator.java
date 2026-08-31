package com.aoo.bcg.profile;

import com.aoo.bcg.media.*;
import java.util.Objects;
import static com.aoo.bcg.profile.PlayerProfileModels.*;

/** The only player-facing composition boundary for avatar media and profile state. */
public final class ProfileMediaOrchestrator {
    private final MediaUploadService media; private final PlayerProfileRepository profiles;
    public ProfileMediaOrchestrator(MediaUploadService media,PlayerProfileRepository profiles){this.media=Objects.requireNonNull(media);this.profiles=Objects.requireNonNull(profiles);}
    public MediaUploadService.Initiated initiate(long owner,String mime,long size,String sha256){return media.initiate(owner,MediaPolicy.Kind.AVATAR,mime,size,0,sha256);}
    public void upload(long owner,String ticketId,int partNumber,byte[] bytes){media.upload(owner,ticketId,partNumber,bytes);}
    public Profile complete(long owner,String ticketId,Long readyAssetId,long expectedVersion){
        if((ticketId==null)==(readyAssetId==null))throw new IllegalArgumentException("exactly one of ticketId or assetId is required");
        long assetId=readyAssetId!=null?readyAssetId:media.complete(owner,ticketId).assetId();
        var asset=media.authorizeRead(owner,assetId);
        if(asset.kind()!=MediaPolicy.Kind.AVATAR)throw new SecurityException("asset is not an avatar");
        return profiles.updateProfile(owner,new ProfilePatch(expectedVersion,null,assetId,null));
    }
}
