package com.aoo.bcg.common.invite;
@FunctionalInterface public interface InviteTelemetry{void record(InviteTraceEvent event);static InviteTelemetry noOp(){return event->{};}}
