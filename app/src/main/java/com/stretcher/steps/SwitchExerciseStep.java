package com.stretcher.steps;


import com.stretcher.Exercise;

/**
 * Switch to a new exercise
 */
public class SwitchExerciseStep implements IStep {
    /**
     * Exercise
     */
    public Exercise exercise;

    /**
     * Total number of actions
     */
    public int numActions;

    /**
     * Number of actions done
     */
    public int numActionsDone;

    public SwitchExerciseStep(Exercise exercise, int numActions) {
        this.exercise = exercise;
        this.numActions = numActions;
        this.numActionsDone = 0;
    }
}