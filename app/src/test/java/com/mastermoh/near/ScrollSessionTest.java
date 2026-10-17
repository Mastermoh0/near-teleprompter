package com.mastermoh.near;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScrollSessionTest {
    @Test public void elapsedTimeControlsSpeedAcrossRefreshRates() {
        ScrollSession at60=runForSecond(60), at120=runForSecond(120);
        assertEquals(60,at60.position(),0.0001);
        assertEquals(at60.position(),at120.position(),0.0001);
    }
    private ScrollSession runForSecond(int refreshRate) {
        ScrollSession session=new ScrollSession(); session.configure(1000,60); session.play();
        for(int i=0;i<=refreshRate;i++) session.tick(Math.round(i*1_000_000_000.0/refreshRate));
        return session;
    }
    @Test public void pauseAndResumeDoNotJumpAfterLongBreak() {
        ScrollSession session=new ScrollSession(); session.configure(1000,100); session.play();
        session.tick(0); session.tick(100_000_000); session.pause();
        session.tick(10_000_000_000L); assertEquals(10,session.position(),0.001);
        session.play(); session.tick(20_000_000_000L); session.tick(20_100_000_000L);
        assertEquals(20,session.position(),0.001);
    }
    @Test public void frameStallIsCappedToAvoidSkippingWords() {
        ScrollSession session=new ScrollSession(); session.configure(1000,100); session.play();
        session.tick(0); session.tick(30_000_000_000L); assertEquals(10,session.position(),0.001);
    }
    @Test public void endStopsAndRestartRewinds() {
        ScrollSession session=new ScrollSession(); session.configure(5,100); session.play();
        session.tick(0); assertFalse(session.tick(100_000_000));
        assertEquals(5,session.position(),0); assertTrue(session.finished());
        session.restart(); assertEquals(0,session.position(),0); assertFalse(session.playing());
        session.play(); assertTrue(session.playing());
    }
    @Test public void manualSeekStaysInsideScript() {
        ScrollSession session=new ScrollSession(); session.configure(200,10);
        session.seek(-5); assertEquals(0,session.position(),0);
        session.seek(300); assertEquals(200,session.position(),0);
        session.configure(100,10); assertEquals(100,session.position(),0);
    }
    @Test public void wordCountHandlesEmptyTextAndUnicodeSpacing() {
        assertEquals(0,ScrollSession.wordCount("\n \t"));
        assertEquals(5,ScrollSession.wordCount("Hello world.\nRead\tthis\u00a0naturally."));
    }
}
