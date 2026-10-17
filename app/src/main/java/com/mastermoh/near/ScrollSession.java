package com.mastermoh.near;

/** Pure scrolling state; timestamps are monotonic and distances are physical pixels. */
final class ScrollSession {
    private double position;
    private double limit;
    private double pixelsPerSecond;
    private long lastFrameNanos = -1;
    private boolean playing;

    static int wordCount(String text) {
        int count = 0;
        boolean insideWord = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            boolean separator = Character.isWhitespace(character) || Character.isSpaceChar(character);
            if (!separator && !insideWord) count++;
            insideWord = !separator;
        }
        return count;
    }
    void configure(double limit, double pixelsPerSecond) {
        this.limit = Math.max(0, limit);
        this.pixelsPerSecond = Math.max(0, pixelsPerSecond);
        position = Math.min(position, this.limit);
        if (finished()) pause();
    }
    void play() { if (!finished()) { playing = true; lastFrameNanos = -1; } }
    void pause() { playing = false; lastFrameNanos = -1; }
    void restart() { pause(); position = 0; }
    void seek(double target) { position = Math.max(0, Math.min(limit, target)); lastFrameNanos = -1; if (finished()) pause(); }
    boolean tick(long frameNanos) {
        if (!playing) return false;
        if (lastFrameNanos >= 0 && frameNanos > lastFrameNanos) {
            double elapsed = Math.min(0.1, (frameNanos - lastFrameNanos) / 1_000_000_000.0);
            position = Math.min(limit, position + elapsed * pixelsPerSecond);
        }
        lastFrameNanos = frameNanos;
        if (finished()) pause();
        return playing;
    }
    double position() { return position; }
    boolean playing() { return playing; }
    boolean finished() { return position >= limit; }
}
