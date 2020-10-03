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

import com.stretcher.steps.ActionStep;
import com.stretcher.steps.FinishedStep;
import com.stretcher.steps.IStep;
import com.stretcher.steps.StartedStep;
import com.stretcher.steps.StepGenerator;
import com.stretcher.steps.SwitchExerciseStep;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    /**
     * Log tag
     */
    private static final String TAG = MainActivity.class.getCanonicalName();


    /**
     * Time elapsed between some work
     */
    private static final long kUPDATE_PERIOD_MS = 50;

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
     * Current exercise action step
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

        mHandler = new Handler(getMainLooper());

        mSteps = StepGenerator.generateSteps(Exercise.load());
        mTotalSteps = mSteps.size();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Wait for TTS to initialize, then start
        mTts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.ERROR) {

                Toast.makeText(this, "TTS not available: status=" + status, Toast.LENGTH_SHORT).show();
                mTts = null;
            } else {
                mTts.setLanguage(Locale.US);
            }


            start();
        });
    }

    /**
     * Start everything
     */
    private void start() {
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonPlayPause).setOnClickListener(view -> MainActivity.this.togglePlayPause());

        // Hide the status bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        // Load up initial exercise
        doWork();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!doWork()) {
                    return;
                }

                mHandler.postDelayed(this, kUPDATE_PERIOD_MS);
            }
        };

        // Start working
        mHandler.post(runnable);
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
        if (mCurrentAction.numWarningBeeps > 0 && mCurrentAction.getRemainingMs() <= (mCurrentAction.numWarningBeeps * 1000)) {
            mCurrentAction.numWarningBeeps -= 1;
            mToneGen.startTone(mCurrentAction.numWarningBeeps > 0 ? ToneGenerator.TONE_CDMA_PIP : ToneGenerator.TONE_DTMF_A, mCurrentAction.numWarningBeeps > 0 ? 150 : 700);
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