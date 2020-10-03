package com.stretcher;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    /**
     * Log tag
     */
    private static final String TAG = MainActivity.class.getCanonicalName();

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
    public static final long kHOLD_DURATION_MS = 15_000;

    interface IStep {
    }

    /**
     * Switch to a new exercise
     */
    static class SwitchExerciseStep implements IStep {
        /**
         * Exercise
         */
        Exercise exercise;

        /**
         * Total number of actions
         */
        int numActions;

        /**
         * Number of actions done
         */
        int numActionsDone;

        public SwitchExerciseStep(Exercise exercise, int numActions) {
            this.exercise = exercise;
            this.numActions = numActions;
            this.numActionsDone = 0;
        }
    }

    /**
     * Exercise action step
     */
    static class ActionStep implements IStep {
        /**
         * Description
         */
        String text;

        /**
         * Duration
         */
        long durationMs;

        /**
         * Timestamp when the state was started
         */
        long startTimeMs;

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
        int mNumWarningBeeps = 3;

        void resetTime() {
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

        boolean isCompleted() {
            return getRemainingMs() <= 0;
        }

        long getRemainingMs() {
            return durationMs - getElapsedTimeMs();
        }

        void togglePause(boolean paused) {
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
    }

    /**
     * Start everything
     */
    static class StartedStep implements IStep {
    }

    /**
     * All done
     */
    static class FinishedStep implements IStep {
    }

    /**
     * Action handler
     */
    private Handler mHandler;

    /**
     * Use to notify the user about current action
     */
    private TextToSpeech mTts;

    /**
     * Used to play warning beeps
     */
    private ToneGenerator mToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    /**
     * Steps to be executed
     */
    List<IStep> mSteps;

    /**
     * Currently executing steps
     */
    IStep mCurrentStep = null;

    /**
     * Number of total steps
     */
    int mTotalSteps;

    /**
     * Current exercise step
     */
    SwitchExerciseStep mCurrentExercise;

    /**
     * Current exercise action stpe
     */
    ActionStep mCurrentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make application fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        // Hide the status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        mHandler = new Handler(getMainLooper());

        mSteps = generateSteps(Exercise.load());
        mTotalSteps = mSteps.size();


        findViewById(R.id.buttonPlayPause).setOnClickListener(view -> MainActivity.this.togglePlayPause());

        mTts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.ERROR) {

                Toast.makeText(this, "TTS not available: status=" + status, Toast.LENGTH_SHORT).show();
                mTts = null;
            } else {
                mTts.setLanguage(Locale.US);
            }

            doWork();

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!doWork()) {
                        return;
                    }

                    mHandler.postDelayed(this, 100);
                }
            };

            mHandler.post(runnable);
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Given a list of exercises, generate a list of steps
     */
    static List<IStep> generateSteps(List<Exercise> exercises) {
        ArrayList<IStep> steps = new ArrayList<>();

        steps.add(new StartedStep());

        for (int exerciseIndex = 0; exerciseIndex < exercises.size(); exerciseIndex++) {
            Exercise exercise = exercises.get(exerciseIndex);

            ArrayList<ActionStep> actions = new ArrayList<>();

            for (int i = 0; i < exercise.numRepetitions; i++) {

                // Do rep
                actions.add(new ActionStep(
                        exercise.bothSides ? "Hold left" : "Hold", kHOLD_DURATION_MS
                ));

                if (exercise.bothSides) {
                    // Rest between sides
                    actions.add(new ActionStep(
                            "Rest", kREP_REST_DURATION_MS
                    ));

                    // Do other side
                    actions.add(new ActionStep("Hold right", kHOLD_DURATION_MS));
                }

                // Rest between reps (if not the last exercise)
                if (i != exercise.numRepetitions - 1) {
                    actions.add(new ActionStep(
                            "Rest", kREP_REST_DURATION_MS
                    ));
                }
            }

            steps.add(new SwitchExerciseStep(exercise, actions.size()));
            steps.addAll(actions);

            if (exerciseIndex != exercises.size() - 1) {
                actions.add(new ActionStep(
                        "Rest", kREST_DURATION_MS
                ));
            }
        }

        steps.add(new FinishedStep());

        return steps;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCurrentAction != null) {
            mCurrentAction.togglePause(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCurrentAction != null) {
            mCurrentAction.togglePause(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        mHandler.removeCallbacksAndMessages(null);

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }

        finish();
    }

    private void togglePlayPause() {
        if (mCurrentAction == null) {
            return;
        }

        mCurrentAction.togglePause(!mCurrentAction.paused);

        ((ImageButton) findViewById(R.id.buttonPlayPause)).setImageResource(
                mCurrentAction.paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause
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

    private boolean doWork() {
        while (mCurrentStep == null) {
            mCurrentStep = mSteps.remove(0);

            ((ProgressBar) findViewById(R.id.totalProgressBar)).setProgress(
                    (int) ((1.0 - ((double) mSteps.size() / (double) mTotalSteps)) * 100)
            );

            // Finished
            if (mCurrentStep instanceof FinishedStep) {
                // All done
                speak("All exercises finished");
                ((TextView) findViewById(R.id.timer)).setText("Done");
                return false;
            } else if (mCurrentStep instanceof SwitchExerciseStep) {
                // New exercise
                mCurrentExercise = ((SwitchExerciseStep) mCurrentStep);

                TextView textView = findViewById(R.id.description);
                textView.setText(mCurrentExercise.exercise.fullDescription);

                ImageView imageView = findViewById(R.id.image);
                imageView.setImageResource(mCurrentExercise.exercise.drawable);

                speak(mCurrentExercise.exercise.name + ". " + mCurrentExercise.exercise.briefDescription);
                mCurrentStep = null;
            } else if (mCurrentStep instanceof ActionStep) {
                mCurrentExercise.numActionsDone++;
                mCurrentAction = (ActionStep) mCurrentStep;
                mCurrentAction.resetTime();

                ((ProgressBar) findViewById(R.id.currentProgressBar)).setProgress(
                        (int) (((double) mCurrentExercise.numActionsDone / mCurrentExercise.numActions) * 100)
                );

                speak(mCurrentAction.text);
                break;
            } else if (mCurrentStep instanceof StartedStep) {
                mCurrentStep = null;
            }
        }

        // Give off warning that state is about to expire
        if (mCurrentAction.mNumWarningBeeps > 0 && mCurrentAction.getRemainingMs() <= (mCurrentAction.mNumWarningBeeps * 1000)) {
            mCurrentAction.mNumWarningBeeps -= 1;
            mToneGen.startTone(mCurrentAction.mNumWarningBeeps > 0 ? ToneGenerator.TONE_CDMA_PIP : ToneGenerator.TONE_DTMF_A, mCurrentAction.mNumWarningBeeps > 0 ? 150 : 700);
        }
        String elapsedTimeStr = formatElapsedTime(mCurrentAction.getRemainingMs());

        if (mCurrentAction.isCompleted()) {
            mCurrentStep = null;
            mCurrentAction = null;
            return true;
        }

        ((TextView) findViewById(R.id.timer)).setText(elapsedTimeStr + "\n" + mCurrentAction.text);

        return true;
    }


    /**
     * Speak some text if TTS is available
     *
     * @param string Text to speak
     */
    private void speak(String string) {
        Log.d(TAG, string);

        if (mTts != null) {
            mTts.speak(string, TextToSpeech.QUEUE_ADD, null);
        }
    }
}