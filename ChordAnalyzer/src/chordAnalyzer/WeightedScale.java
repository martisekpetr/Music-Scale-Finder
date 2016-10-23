package chordAnalyzer;

/**
 * Extension of a {@link Scale} object with specific root {@link Tone} and accuracy computed from the input chords.
 */
public class WeightedScale extends Scale implements Comparable<WeightedScale> {

        private Tone root;
        private double accuracy;

        /**
         * Default constructor. Takes a {@link Scale} object, a {@link Tone} object defining the root of the scale and the scale accuracy given as a floating point number between 0 and 1.
         * 
         * @param scale Scale object from which a specific WeightedScale is constructed.
         * @param root root tone of the scale.
         * @param accuracy number from 0 to 1 representing how much does the scale match given chords.
         */
        public WeightedScale(Scale scale, Tone root, double accuracy) {
            super(scale.toString(), scale.getMask());
            this.root = root;
            this.accuracy = accuracy;

        }

        /**
         * Returns root tone of this scale and its name separated by space.
         * @return root and name of the scale separated by space.
         */
        @Override
        public String toString() {
            return root.toString() + " " + super.toString();
        }

        /**
         * Gets accuraccy of this WeightedScale object.
         *
         * @return accuracy in percent
         */
        public int getAccuracy() {
            return (int) (accuracy * 100);
        }
        
        /**
         * Gets root tone of this scale.
         * @return root tone of the scale
         */
        public Tone getRoot(){
            return root;
        }

        /**
         * Gets array of {@link Tone}s in this scale.
         * @return array of {@link Tone}
         */
        public Tone[] getTones() {
            Tone[] result = new Tone[super.getMask().length];
            int counter = 0;
            for (int i : super.getMask()) {
                result[counter++] = Tone.values()[(root.ordinal() + i) % 12];
            }
            return result;
        }

        /**
         * Compares this WeightedScale with another WeightedScale by their accuracy.
         * @param wscale a WeightedScale object to be compared with this
         * @return difference of accuracies of compared scales
         */
        @Override
        public int compareTo(WeightedScale wscale) {
            return this.getAccuracy() - wscale.getAccuracy();
        }
    }
