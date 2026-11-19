package com.ddm.server.common;

import java.util.Set;

public interface IGameApp {
    /**
     * 加载头部包
     *
     * @return
     */
    public abstract Set<Class<?>> loadHandLer();
}
