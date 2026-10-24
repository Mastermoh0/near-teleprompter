package com.mastermoh.near;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseVersionTest {
    @Test public void acceptsStableTagsAndInstalledVersions() {
        assertEquals("1.2.0",ReleaseVersion.parse("v1.2.0").toString());
        assertEquals(0,ReleaseVersion.parse("1.2.0").compareTo(ReleaseVersion.parse("v1.2.0")));
    }
    @Test public void comparesNumbersNotCharacters() { assertTrue(ReleaseVersion.parse("1.10.0").compareTo(ReleaseVersion.parse("1.9.9"))>0); }
    @Test public void majorAndPatchVersionsAreOrdered() {
        assertTrue(ReleaseVersion.parse("2.0.0").compareTo(ReleaseVersion.parse("1.99.99"))>0);
        assertTrue(ReleaseVersion.parse("1.2.1").compareTo(ReleaseVersion.parse("1.2.0"))>0);
        assertTrue(ReleaseVersion.parse("1.1.0").compareTo(ReleaseVersion.parse("1.2.0"))<0);
    }
    @Test public void rejectsPrereleasesAndUntrustedTags() {
        for(String bad:new String[]{"1.2.0-beta","v1.2.0+build","https://example.com","../v1.2.0"," 1.2.0","1.2","01.2.0","9999999.0.0",""}) assertNull(bad,ReleaseVersion.parse(bad));
        assertNull(ReleaseVersion.parse(null));
    }
}
