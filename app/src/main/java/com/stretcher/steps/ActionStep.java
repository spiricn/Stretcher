package com.stretcher.steps;

/**
 * Exercise action step
 */
public class ActionStep implements IStep {
    /**
     * Description
     */
    public String text;

    /**
     * Duration
     */
    public long durationMs;

    /**
     * Timestamp when the state was started
     */
    public long startTimeMs;

    /**
     * Indication if session is paused or not
     */
    public boolean paused = false;

    /**
     * Timestamp when the session was paused
     */
    public long pausedTimeMs = 0;

    /**
     * How many warning beeps left to be played (one per each second before state is finished)
     */
    public int numWarningBeeps = 3;

    public void resetTime() {
        startTimeMs = System.currentTimeMillis();
    }

    public ActionStep(String text, long durationMs) {
        this.text = text;
        this.durationMs = durationMs;
    }

    long getElapsedTimeMs() {
        if (paused) {
            return pausedTimeMs - startTimeMs;
        } else {
            return System.currentTimeMillis() - startTimeMs;
        }
    }

    public boolean isCompleted() {
        return getRemainingMs() <= 0;
    }

    public long getRemainingMs() {
        return durationMs - getElapsedTimeMs();
    }

    public void togglePause(boolean paused) {
        if (this.paused == paused) {
            return;
        }

        this.paused = paused;

        if (this.paused) {
            pausedTimeMs = System.currentTimeMillis();
        } else {
            startTimeMs = System.currentTimeMillis() - (pausedTimeMs - startTimeMs);
        }
    }

    @Override
    public String toString() {
        return "[ActionStep " + this.text + " , " + this.durationMs + "]";
    }
}