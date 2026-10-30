package com.ddm.server.common.lock;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Data
@NoArgsConstructor
public class AtomicBooleanLock<T> implements Serializable {
    private final ReentrantLock lock = new ReentrantLock(true);

    public boolean booleanLock(LockImpl lock) {
        boolean acquired = false;
        try { acquired = this.lock.tryLock(5, TimeUnit.SECONDS); if (!acquired) throw new IllegalStateException("lock timeout"); return lock.run(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("lock interrupted", interrupted); }
        finally { if (acquired) this.lock.unlock(); }
    }

    public T booleanValueLock(LockValueImpl lock) {
        boolean acquired = false;
        try { acquired = this.lock.tryLock(5, TimeUnit.SECONDS); if (!acquired) throw new IllegalStateException("lock timeout"); return (T) lock.run(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("lock interrupted", interrupted); }
        finally { if (acquired) this.lock.unlock(); }
    }
}
