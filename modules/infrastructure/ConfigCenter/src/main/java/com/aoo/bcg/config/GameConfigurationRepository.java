package com.aoo.bcg.config;
import java.util.Optional;
public interface GameConfigurationRepository { <C> Optional<PublishedGameConfiguration<C>> find(int gameId, String playVersion); }
