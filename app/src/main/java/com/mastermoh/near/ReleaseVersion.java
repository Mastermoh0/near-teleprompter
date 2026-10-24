package com.mastermoh.near;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private static final Pattern STABLE = Pattern.compile("v?(0|[1-9]\\d{0,5})\\.(0|[1-9]\\d{0,5})\\.(0|[1-9]\\d{0,5})");
    final int major, minor, patch;
    private ReleaseVersion(int major,int minor,int patch) { this.major=major; this.minor=minor; this.patch=patch; }
    static ReleaseVersion parse(String text) {
        Matcher match=STABLE.matcher(text == null ? "" : text);
        return match.matches() ? new ReleaseVersion(Integer.parseInt(match.group(1)),Integer.parseInt(match.group(2)),Integer.parseInt(match.group(3))) : null;
    }
    @Override public int compareTo(ReleaseVersion other) {
        if(major!=other.major) return Integer.compare(major,other.major);
        if(minor!=other.minor) return Integer.compare(minor,other.minor);
        return Integer.compare(patch,other.patch);
    }
    @Override public String toString() { return major+"."+minor+"."+patch; }
}
