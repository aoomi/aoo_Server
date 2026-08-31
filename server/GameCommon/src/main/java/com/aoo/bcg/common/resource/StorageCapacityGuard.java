package com.aoo.bcg.common.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.ToLongFunction;

/** Shared fail-fast capacity gate for every disk-writing runtime domain. */
public final class StorageCapacityGuard {
    public enum Domain { LOG, REPLAY, UPLOAD, BUILD, DATABASE_TEMP, CONFIGURATION }
    private static final long DEFAULT_RESERVE_BYTES = 256L * 1024 * 1024;
    private static final StorageCapacityGuard SYSTEM = new StorageCapacityGuard(StorageCapacityGuard::usableSpace);
    private final ToLongFunction<Path> capacity;

    public StorageCapacityGuard() { this(StorageCapacityGuard::usableSpace); }
    StorageCapacityGuard(ToLongFunction<Path> capacity) { this.capacity = Objects.requireNonNull(capacity); }
    public static StorageCapacityGuard system() { return SYSTEM; }

    public void require(Path destination, long requestedBytes, Domain domain) {
        require(destination, requestedBytes, DEFAULT_RESERVE_BYTES, domain);
    }

    public void require(Path destination, long requestedBytes, long reserveBytes, Domain domain) {
        if (requestedBytes < 0 || reserveBytes < 0) throw new IllegalArgumentException("disk capacity bounds must be non-negative");
        Path probe = existingAncestor(destination.toAbsolutePath());
        long usable = capacity.applyAsLong(probe);
        if (usable < requestedBytes || usable - requestedBytes < reserveBytes)
            throw new StorageCapacityException(domain, requestedBytes, reserveBytes, usable);
    }

    private static Path existingAncestor(Path path) { Path current=path;while(current!=null&&!Files.exists(current))current=current.getParent();if(current==null)throw new IllegalArgumentException("destination has no existing ancestor");return current; }
    private static long usableSpace(Path path) { try { return Files.getFileStore(path).getUsableSpace(); } catch (IOException error) { throw new IllegalStateException("cannot read usable disk space", error); } }

    public static final class StorageCapacityException extends IllegalStateException {
        private final Domain domain;
        StorageCapacityException(Domain domain,long requested,long reserve,long usable){super("insufficient disk capacity domain="+domain+" requested="+requested+" reserve="+reserve+" usable="+usable);this.domain=domain;}
        public Domain domain(){return domain;}
    }
}
