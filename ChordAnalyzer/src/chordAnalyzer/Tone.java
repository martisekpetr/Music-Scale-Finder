package chordAnalyzer;

/**
 * Enumeration of all twelve musical tones. The method <code>toString</code> is redefined to return musical representation of tones, using "#" and "b" symbols, whereas
 * internal representation uses a rather non-elegant "-is" notation. The sharpened version of tones are used (i.e. "C#" instead of "Db") regardless of a scale for simplicity.
 * The one exception is a "Bb" tone - for eliminating the confusion rising from different English (... A, A#/Bb, B) and Czech (A, A#/B, H) notation. Resulting notation is not 100% 
 * musically correct, but it is unambiguous.
 */
public enum Tone {

    C, Cis, D, Dis, E, F, Fis, G, Gis, A, Bb, B;

    /**
     * Returns musical notation of each Tone using "sharp" symbol # instead of
     * -is suffix.
     * @return String representation of a musical tone
     */
    @Override
    public String toString() {
        switch (this) {
            case C:
                return "C";
            case Cis:
                return "C#";
            case D:
                return "D";
            case Dis:
                return "D#";
            case E:
                return "E";
            case F:
                return "F";
            case Fis:
                return "F#";
            case G:
                return "G";
            case Gis:
                return "G#";
            case A:
                return "A";
            case Bb:
                return "Bb";
            case B:
                return "B";
            default:
                return null;
        }
    }
}