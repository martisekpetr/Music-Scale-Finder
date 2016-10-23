package chordAnalyzer;

/**
 * Structure representing a musical scale with no specific root {@link Tone}. A
 * scale is identified by its name and defined by its tonal mask.
 */
public class Scale {

    private int[] mask;
    private String name;

    /**
     * Default constructor.
     *
     * @param name name of a scale.
     * @param mask tonal mask of a scale.
     */
    public Scale(String name, int[] mask) {
        this.mask = mask;
        this.name = name;
    }

    /**
     * Gets tonal mask of this scale.
     *
     * @return tonal mask of the scale
     */
    public int[] getMask() {
        return mask;
    }

    /**
     * Returns the name of this scale.
     * @return name of the scale
     */
    @Override
    public String toString() {
        return name;
    }
}