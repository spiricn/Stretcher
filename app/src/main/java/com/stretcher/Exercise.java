package com.stretcher;

import java.util.ArrayList;
import java.util.List;

public class Exercise {
    /**
     * Name
     */
    public String name;

    /**
     * Description
     */
    public String fullDescription;

    /**
     * Brief description
     */
    public String briefDescription;

    /**
     * Image depicting the excersie
     */
    public int drawable;

    /**
     * Indication if each rep should be repeated for both sides (e.g. left and right)
     */
    public boolean bothSides = false;

    /**
     * How many reps
     */
    public int numRepetitions;

    Exercise(String name, String briefDescription, String fullDescription, int drawable, boolean bothSides, int repetitions) {
        this.name = name;
        this.briefDescription = briefDescription;
        this.fullDescription = fullDescription;
        this.drawable = drawable;
        this.bothSides = bothSides;
        this.numRepetitions = repetitions;
    }

    /**
     * Load a list of exercises
     */
    public static List<Exercise> load() {
        List<Exercise> entries = new ArrayList<>();

        entries.add(new Exercise(
                "Neck Retractions",
                "Head back, eyes on the horizon.",
                "While sitting down, bring head straight back, keeping your eyes on the horizon. Then return to neutral.",
                R.drawable.neck_retraction,
                false,
                10));

        entries.add(new Exercise(
                "Head Drop",
                "Look up",
                "Starting in a seated position, retract neck (as shown in picture). Slowly move head up, and backward as far as you can comfortably go. Return to neutral.",
                R.drawable.head_drop,
                false,
                10));

        entries.add(new Exercise(
                "Side Bend",
                "Pull head left and right",
                "Sit down, bring head into neck-retraction position, then gently guide right ear toward right shoulder with right hand. \n"
                        + "Stop when you feel a stretch on the left side of neck. Return to neutral. Repeat for other side.",
                R.drawable.side_bend,
                true,
                5));

        entries.add(new Exercise(
                "Rotation",
                "Look left, then right.",
                "While sitting, bring head back into neck-retraction position, then gently turn head diagonally to the right so your nose is over your shoulder. Return to neutral. Repeat in other direction.",
                R.drawable.rotation,
                true,
                5));

        entries.add(new Exercise(
                "Flexion",
                "Clasp head and push down",
                "Sitting down, bring head into neck-retraction position. Clasp hands behind head and gently guide head down, bringing chin toward chest. Stop when you feel a stretch in the back of your neck. Return to neutral.",
                R.drawable.flexion,
                false,
                5));

        entries.add(new Exercise(
                "Should Blade Pull",
                "Bend arms behind back.",
                "While sitting, bend raised arms at 90-degree angles. Relax shoulders and neck. Keeping arms and neck still, squeeze the muscles between shoulder blades drawing shoulder blades closer together. Return to neutral.",
                R.drawable.should_blade_pull,
                false,
                5));

        return entries;

    }
}
