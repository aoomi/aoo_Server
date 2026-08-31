package com.ddm.server.dispatcher.disruptor;

import com.lmax.disruptor.EventFactory;

public class BufferEventFactory implements EventFactory<MessageBuffer> {
    @Override
    public MessageBuffer newInstance() {
        return new MessageBuffer();
    }
}
