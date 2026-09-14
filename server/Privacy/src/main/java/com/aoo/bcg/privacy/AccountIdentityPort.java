package com.aoo.bcg.privacy;
@FunctionalInterface public interface AccountIdentityPort {
 long authenticate(String bearer,String device,String channel,String version,String ip);
}
