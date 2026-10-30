package com.aoo.bcg.config;

public interface GameConfigurationCodec<C> {
    String encode(PublishedGameConfiguration<C> configuration);
    PublishedGameConfiguration<C> decode(String payload);
}
