//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package core.dispatcher;

import BaseCommon.CommLog;

import java.io.IOException;

public abstract class IServerBaseHandler {
    private String event = "";

    public IServerBaseHandler() {
        try {
            event = getClass().getName().replaceAll("^.*\\.handler\\.", "").toLowerCase();
        } catch (Exception e) {
            CommLog.error("error registry handler name for {}", getClass().getName());
        }
    }

    public IServerBaseHandler(short opCode, String opName) {
        this.event = opName;
    }

    public IServerBaseHandler(String name) {
        this((short) 0, name);
    }

    public String getEvent() {
        return event;
    }

    public abstract void handleMessage(final String data) throws IOException;


}
