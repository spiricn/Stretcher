package com.stretcher;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    enum State {
        /**
         * rest between reps
         */
        REP_REST,

        /**
         * rest between exercises
         */
        REST,

        /**
         * hold position
         */
        HOLD,
    }

    /**
     * Rest between exercises
     */
    private static final long kREST_DURATION_MS = 10_000;

    /**
     * Rest between reps
     */
    private static final long kREP_REST_DURATION_MS = 5000;

    /**
     * How long should the position be held for
     */
    public long kHOLD_DURATION_MS = 15_000;

    static class ExerciseSession {
        /**
         * Number of reps done
         */
        int numRepsDone = 0;

        /**
         * Current exercise
         */
        Exercise exercise;

        /**
         * Current state
         */
        State state = State.REST;

        /**
         * Timestamp when the state was started
         */
        long startTimeMs;

        /**
         * Total state duration
         */
        long durationMs;

        /**
         * Indication if the first side is being held
         */
        boolean firstSide = true;

        /**
         * Indication if session is paused or not
         */
        boolean paused = false;

        /**
         * Timestamp when the session was paused
         */
        long pausedTimeMs = 0;

        /**
         * How many warning beeps left to be played (one per each second before state is finished)
         */
        int mNumWarningBeeps = 0;

        ExerciseSession(Exercise exercise) {
            this.exercise = exercise;
            resetTime();
        }

        void resetTime() {
            startTimeMs = System.currentTimeMillis();
        }

        long getElapsedTimeMs() {
            if (paused) {
                return pausedTimeMs - startTimeMs;
            } else {
                return System.currentTimeMillis() - startTimeMs;
            }
        }

        boolean isCompleted() {
            return getRemainingMs() <= 0;
        }

        long getRemainingMs() {
            return durationMs - getElapsedTimeMs();
        }

        void togglePause() {
            paused = !paused;

            if (paused) {
                pausedTimeMs = System.currentTimeMillis();
            } else {
                startTimeMs = System.currentTimeMillis() - (pausedTimeMs - startTimeMs);
            }
        }


        void setState(State state, long durationMs) {
            this.state = state;
            this.durationMs = durationMs;

            resetTime();

            mNumWarningBeeps = 3;
        }
    }

    /**
     * Current session
     */
    private ExerciseSession mSession = null;

    /**
     * Action handler
     */
    private Handler mHandler;

    /**
     * All available exercises
     */
    private List<Exercise> mEntries;

    /**
     * Use to notify the user about current action
     */
    private TextToSpeech mTts;

    /**
     * Used to play warning beeps
     */
    private ToneGenerator mToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(getMainLooper());

        mEntries = Exercise.load();

        findViewById(R.id.buttonPlayPause).setOnClickListener(view -> MainActivity.this.playPause());

        mTts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.ERROR) {
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
                finish();
                return;
            }

            mTts.setLanguage(Locale.US);

            playPause();
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onPause() {
        super.onPause();

        mHandler.removeCallbacksAndMessages(null);

        mTts.stop();
        mTts.shutdown();

        finish();
    }

    private void playPause() {
        if (mSession == null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!step()) {
                        return;
                    }

                    mHandler.postDelayed(this, 100);
                }
            };

            startExercise(0);
            mSession.setState(State.REP_REST, kREP_REST_DURATION_MS);

            mHandler.post(runnable);

            return;
        }

        mSession.togglePause();

        ((ImageButton) findViewById(R.id.buttonPlayPause)).setImageResource(
                mSession.paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause
        );
    }

    @SuppressLint("DefaultLocale")
    private static String formatElapsedTime(long durationMs) {
        long milliseconds = durationMs % 1000;
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / 1000) / 60;

        return String.format("%02d:%02d.%03d",
                minutes, seconds, milliseconds);
    }

    private boolean step() {
        // Give off warning that state is about to expire
        if (mSession.mNumWarningBeeps > 0 && mSession.getRemainingMs() <= (mSession.mNumWarningBeeps * 1000)) {
            mSession.mNumWarningBeeps -= 1;
            mToneGen.startTone(mSession.mNumWarningBeeps > 0 ? ToneGenerator.TONE_CDMA_PIP : ToneGenerator.TONE_DTMF_A, mSession.mNumWarningBeeps > 0 ? 150 : 700);
        }

        if (mSession.isCompleted()) {
            switch (mSession.state) {
                case REST:
                case REP_REST:
                    mSession.setState(State.HOLD, kHOLD_DURATION_MS);

                    String side = "";

                    if (mSession.exercise.bothSides) {
                        side += mSession.firstSide ? " left " : " right ";
                    }

                    speak("Hold " + side + " for " + (kHOLD_DURATION_MS / 1000) + " seconds");

                    break;

                case HOLD:
                    if (mSession.exercise.bothSides && mSession.firstSide) {
                        mSession.firstSide = false;
                    } else {
                        mSession.numRepsDone++;
                        mSession.firstSide = true;
                    }

                    if (mSession.numRepsDone == mSession.exercise.numRepetitions) {
                        int nextIndex = mEntries.indexOf(mSession.exercise) + 1;

                        if (nextIndex == mEntries.size()) {
                            // All done
                            speak("All exercises finished");
                            ((TextView) findViewById(R.id.timer)).setText("Done");
                            return false;
                        }

                        // Move to next exercise
                        startExercise(nextIndex);

                        mSession.setState(State.REST, kREST_DURATION_MS);

                    } else {
                        // Rest between reps
                        mSession.setState(State.REP_REST, kREP_REST_DURATION_MS);
                        speak("Rest for " + (mSession.durationMs / 1000) + " seconds");
                    }

                    break;
            }
        }

        String elapsedTimeStr = formatElapsedTime(mSession.getRemainingMs());

        String status;

        if (mSession.state == State.HOLD) {
            status = "Hold " + (mSession.numRepsDone + 1) + "/" + mSession.exercise.numRepetitions;
        } else {
            status = "Rest ..";
        }

        ((TextView) findViewById(R.id.timer)).setText(elapsedTimeStr + "\n" + status);

        return true;
    }


    void startExercise(int index) {
        mSession = new ExerciseSession(mEntries.get(index));

        Exercise entry = mEntries.get(index);

        ((TextView) findViewById(R.id.description)).setText(entry.fullDescription);
        ((ImageView) findViewById(R.id.image)).setImageResource(entry.drawable);

        speak(mSession.exercise.name + ". " + mSession.exercise.briefDescription);
    }

    private void speak(String string) {
        mTts.speak(string, TextToSpeech.QUEUE_ADD, null);
    }
}