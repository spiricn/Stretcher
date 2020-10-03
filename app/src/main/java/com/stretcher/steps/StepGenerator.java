package com.stretcher.steps;

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

            // Rest after the exercise
            if (exerciseIndex != exercises.size() - 1) {
                actions.add(new ActionStep(
                        "Rest", kREST_DURATION_MS
                ));
            }
        }

        steps.add(new FinishedStep());

        return steps;
    }

}
