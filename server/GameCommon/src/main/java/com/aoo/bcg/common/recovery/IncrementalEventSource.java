package com.aoo.bcg.common.recovery;
import java.util.List;
public interface IncrementalEventSource<E> { List<E> after(long roomId, long sequenceExclusive); }
