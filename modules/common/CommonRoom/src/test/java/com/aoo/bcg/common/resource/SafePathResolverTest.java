package com.aoo.bcg.common.resource;
import static org.junit.jupiter.api.Assertions.*;import java.nio.file.*;import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
class SafePathResolverTest{@TempDir Path root;
 @Test void acceptsOwnedRelativePaths(){assertEquals(root.resolve("safe/a.txt").toAbsolutePath(),SafePathResolver.relative(root,"safe/a.txt"));assertEquals(root.resolve("upload.bin").toAbsolutePath(),SafePathResolver.fileName(root,"upload.bin"));}
 @Test void rejectsTraversalAbsoluteEncodedAndWindowsForms(){for(String path:java.util.List.of("../x","safe/../../x","/etc/passwd","C:\\temp\\x","%2e%2e/x","safe%2fx"))assertThrows(SecurityException.class,()->SafePathResolver.relative(root,path),path);}
 @Test void rejectsSymlinkComponent()throws Exception{Path outside=Files.createTempDirectory("outside-");Files.createSymbolicLink(root.resolve("link"),outside);assertThrows(SecurityException.class,()->SafePathResolver.relative(root,"link/file"));Files.delete(outside);}
 @Test void uploadNamesCannotContainDirectories(){for(String path:java.util.List.of("a/b","..",".hidden","a%2fb"))assertThrows(SecurityException.class,()->SafePathResolver.fileName(root,path));}
}
