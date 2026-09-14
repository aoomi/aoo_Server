package com.aoo.bcg.common.resource;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves untrusted upload, archive, replay and resource-import names inside an owned root. */
public final class SafePathResolver {
    private SafePathResolver() {}
    public static Path fileName(Path root,String name){if(name==null||!name.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")||name.equals(".")||name.equals(".."))throw new SecurityException("invalid untrusted file name");return relative(root,name);}
    public static Path relative(Path root,String value){
        if(value==null||value.isBlank()||value.indexOf('\0')>=0||value.indexOf('\\')>=0)throw new SecurityException("invalid relative path");
        String lower=value.toLowerCase(Locale.ROOT);if(lower.contains("%2e")||lower.contains("%2f")||lower.contains("%5c"))throw new SecurityException("encoded traversal is forbidden");
        Path base=root.toAbsolutePath().normalize();Path candidate=Path.of(value);if(candidate.isAbsolute())throw new SecurityException("absolute path is forbidden");Path resolved=base.resolve(candidate).normalize();if(!resolved.startsWith(base))throw new SecurityException("path escapes owned root");
        Path current=base;for(Path segment:base.relativize(resolved)){current=current.resolve(segment);if(Files.exists(current,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(current))throw new SecurityException("symbolic-link path component is forbidden");}
        return resolved;
    }
}
