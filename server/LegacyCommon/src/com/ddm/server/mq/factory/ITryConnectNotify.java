package com.ddm.server.mq.factory;

public interface ITryConnectNotify {

    public void registryNotify();

    public void serverNotify();

    public void stopCurConnect();
}
