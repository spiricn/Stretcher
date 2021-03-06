package com.stretcher.steps;

import android.util.Log;

import com.stretcher.Exercise;

import java.util.ArrayList;
import java.util.List;

public class StepGenerator {
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
    private static final long kHOLD_DURATION_MS = 15_000;

    /**
     * Given a list of exercises, generate a list of steps
     */
    public static List<IStep> generateSteps(List<Exercise> exercises) {
        ArrayList<IStep> steps = new ArrayList<>();

        steps.add(new StartedStep());

        for (int exerciseIndex = 0; exerciseIndex < exercises.size(); exerciseIndex++) {
            Exercise exercise = exercises.get(exerciseIndex);

            // Generate actions for this exercise
            List<IStep> actions = generateExerciseActions(exercise);

            // Switch exercise
            steps.add(new SwitchExerciseStep(exercise, actions.size()));

            // Rest before first hold
            steps.add(generateRest(exerciseIndex == 0 ? kREP_REST_DURATION_MS : kREST_DURATION_MS));

            // Do actions
            steps.addAll(actions);
        }

        steps.add(new FinishedStep());

        return steps;
    }

    private static IStep generateRest(long durationMs) {
        return new ActionStep("Rest", durationMs);
    }

    private static List<IStep> generateExerciseActions(Exercise exercise) {
        ArrayList<IStep> actions = new ArrayList<>();
        for (int i = 0; i < exercise.numRepetitions; i++) {

            // Do rep
            actions.add(new ActionStep(
                    exercise.bothSides ? "Hold left" : "Hold", kHOLD_DURATION_MS
            ));

            if (exercise.bothSides) {
                // Rest between sides
                actions.add(generateRest(kREP_REST_DURATION_MS));

                // Do other side
                actions.add(new ActionStep("Hold right", kHOLD_DURATION_MS));
            }

            // Rest between reps (if not the last exercise)
            if (i != exercise.numRepetitions - 1) {
                actions.add(generateRest(kREP_REST_DURATION_MS));
            }
        }

        return actions;
    }

}
