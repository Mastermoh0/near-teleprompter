package com.mastermoh.near;

import org.junit.Test;
import static org.junit.Assert.*;

public class SharedTextTest {
    @Test public void normalizesParagraphsWithoutInterpretingMarkup() {
        assertEquals("<b>A</b>\n\nB\nC",SharedText.normalize("<b>A</b>\r\n\r\nB\rC\u0000"));
    }
    @Test public void rejectsEmptyAndWhitespace() {
        assertEquals("",SharedText.normalize(null));
        assertFalse(SharedText.valid(" \n\t\u2003"));
    }
    @Test public void enforcesLimitWithoutTruncatingTheScript() {
        String full="A".repeat(100000);
        assertTrue(SharedText.valid(full));
        assertFalse(SharedText.valid(full+"B"));
        assertEquals(100001,SharedText.normalize(full+"B").length());
    }
    @Test public void titlesStaySingleLineAndBounded() {
        assertEquals("A B",SharedText.title(" A\nB "));
        assertEquals(80,SharedText.title("A".repeat(100)).length());
        assertEquals(79,SharedText.title("A".repeat(79)+"\uD83D\uDE00").length());
    }
    @Test public void urlsRemainLiteralOfflineText() {
        assertEquals("https://example.com/script",SharedText.normalize("https://example.com/script"));
        assertTrue(SharedText.valid("https://example.com/script"));
    }
}
