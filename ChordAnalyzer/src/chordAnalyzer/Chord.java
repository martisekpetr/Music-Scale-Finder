package chordAnalyzer;

/**
 * Structure representing a chord specified by a root tone and a name. Unlike {@link Scale}, the tonal mask of a chord is not stored in the <code>Chord</code> object, because it 
 * would be redundant. All known chord shapes are already stored in a static variable of the main class {ChordAnalyzer} in a <code>HashMap</code>, because they are needed
 * during a construction of the GUI, in combo boxes in chord input panel. Therefore any changes in the <code>chords</code> file require a restart of the application to take effect, 
 * while changes in <code>scales</code> file take effect immediately during next analysis.
 */
public class Chord {

    private String name;
    private Tone root;

    /**
     * Default constructor.
     * @param root root {@link Tone} of the Chord
     * @param name name of the Chord
     */
    public Chord(Tone root, String name) {
        this.root = root;
        this.name = name;
    }

    /**
     * Gets name of the Chord.
     * @return name of the Chord
     */
    public String getName() {
        return name;
    }

    /**
     * Gets root tone of this Chord.
     * @return root tone of the Chord
     */
    public Tone getRoot(){
        return root;
    }
}
