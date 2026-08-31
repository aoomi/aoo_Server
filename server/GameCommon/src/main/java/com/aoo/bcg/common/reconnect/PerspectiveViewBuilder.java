package com.aoo.bcg.common.reconnect;
public interface PerspectiveViewBuilder<S, V> { V build(S authoritativeState, long authenticatedViewerId); }
