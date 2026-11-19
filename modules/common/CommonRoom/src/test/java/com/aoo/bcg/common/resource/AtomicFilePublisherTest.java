package com.aoo.bcg.common.resource;
import static org.junit.jupiter.api.Assertions.*;import java.nio.file.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
class AtomicFilePublisherTest{@TempDir Path directory;
 @Test void checksumsAndAtomicallyReplacesDestination()throws Exception{Path target=directory.resolve("config.json");byte[]first="one".getBytes();AtomicFilePublisher.write(target,first,AtomicFilePublisher.sha256(first));assertEquals("one",Files.readString(target));byte[]second="two".getBytes();AtomicFilePublisher.write(target,second,AtomicFilePublisher.sha256(second));assertEquals("two",Files.readString(target));assertEquals(0,Files.list(directory).filter(path->path.toString().endsWith(".tmp")).count());}
 @Test void rejectsCorruptStagedFileWithoutReplacingDestination()throws Exception{Path target=directory.resolve("replay.bin");Files.writeString(target,"old");Path staged=directory.resolve("stage.tmp");Files.writeString(staged,"new");assertThrows(IllegalArgumentException.class,()->AtomicFilePublisher.publish(staged,target,"0".repeat(64)));assertEquals("old",Files.readString(target));}
}
