package com.aoo.bcg.common.resource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Crash-safe publication for configuration, replay, download and generated runtime files. */
public final class AtomicFilePublisher {
    private AtomicFilePublisher() {}

    public static void write(Path destination, byte[] body, String expectedSha256) {
        Path parent = destination.toAbsolutePath().getParent();
        Path temporary = null;
        try {
            StorageCapacityGuard.system().require(destination, body.length, StorageCapacityGuard.Domain.CONFIGURATION);
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, destination.getFileName() + ".", ".tmp");
            Files.write(temporary, body, StandardOpenOption.TRUNCATE_EXISTING);
            force(temporary);
            publish(temporary, destination, expectedSha256);
        } catch (RuntimeException error) { cleanup(temporary, error); throw error; }
        catch (Exception error) { cleanup(temporary, error); throw new IllegalStateException("atomic file write failed", error); }
    }

    public static void publish(Path staged, Path destination, String expectedSha256) {
        try {
            String actual = sha256(staged);
            if (!MessageDigest.isEqual(actual.getBytes(), expectedSha256.getBytes())) throw new IllegalArgumentException("staged file checksum mismatch");
            Files.createDirectories(destination.toAbsolutePath().getParent());
            force(staged);
            Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            forceDirectory(destination.toAbsolutePath().getParent());
        } catch (RuntimeException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException("atomic file publication failed", error); }
    }

    public static String sha256(Path path) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
        catch (Exception error) { throw new IllegalStateException("cannot checksum file", error); }
    }

    public static String sha256(byte[] body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)); }
        catch (Exception error) { throw new IllegalStateException("cannot checksum bytes", error); }
    }

    private static void force(Path path) throws IOException { try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) { channel.force(true); } }
    private static void forceDirectory(Path path) { try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) { channel.force(true); } catch (IOException ignored) { /* unsupported on some file systems */ } }
    private static void cleanup(Path path, Throwable error) { if (path != null) try { Files.deleteIfExists(path); } catch (Exception cleanup) { error.addSuppressed(cleanup); } }
}
