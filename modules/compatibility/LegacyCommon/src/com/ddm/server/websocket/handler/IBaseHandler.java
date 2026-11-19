package com.ddm.server.websocket.handler;

import com.ddm.server.common.CommLogD;


public abstract class IBaseHandler {

    private String event = "";

    public IBaseHandler() {
        try {
            event = getClass().getName().replaceAll("^.*\\.handler\\.", "").toLowerCase();
            CommLogD.debug("event: {}", event);
        } catch (Exception e) {
            CommLogD.error("error handler name for {}", getClass().getName());
        }
    }

    public IBaseHandler(short opCode, String opName) {
        this.event = opName;
    }

    public IBaseHandler(String name) {
        this((short) 0, name);
    }

    public String getEvent() {
        return event;
    }
}
