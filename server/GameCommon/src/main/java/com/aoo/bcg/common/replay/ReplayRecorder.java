package com.aoo.bcg.common.replay;
import java.util.List;
public interface ReplayRecorder { void append(long roomId, ReplayFrame frame); List<ReplayFrame> load(long roomId); }
