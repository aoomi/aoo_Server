package com.aoo.bcg.gamespi.api;

public record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
    public SemanticVersion { if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("negative semantic version"); }
    public static SemanticVersion parse(String value) {
        if (value == null || !value.matches("\\d+\\.\\d+\\.\\d+")) throw new IllegalArgumentException("invalid semantic version");
        String[] parts = value.split("\\.");
        return new SemanticVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
    @Override public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        return result == 0 ? Integer.compare(patch, other.patch) : result;
    }
    @Override public String toString() { return major + "." + minor + "." + patch; }
}
