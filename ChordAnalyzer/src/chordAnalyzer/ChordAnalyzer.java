package chordAnalyzer;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.*;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * Main class encapsulating all the functionality and GUI. The method
 * <code>main</code> sets up GUI. Listener for buttons for adding and removing
 * chord input interfaces perform the desired actions. Listener for button which
 * triggers the analysis creates an array of
 * <code>Chord</code> objects, calls
 * <code>findScales</code> method, which loads all the known scales from the
 * file
 * <code>scales</code> located in the application directory using
 * <code>loadScales</code> method and then compares them against the given
 * <code>Chord</code> array, returning the matching ones in a
 * <code>List</code> of
 * <code>WeightedScale</code> objects. This
 * <code>List</code> is then sorted by descending accuracy and displayed using
 * <code>displayScales</code> method, which updates the model of the
 * <code>JTable</code> object
 * <code>scalesTable</code>.
 * <br/>
 * <code>ScaleSelectedListener</code> is triggered when user selects a scale
 * from the table, updating a display in bottom half of the window. Graphic
 * elements displaying detials about a single scale are encapsulated in a helper
 * class
 * <code>singleScaleDisplay</code>, which provides the method
 * <code>updateDisplayedScale</code>.
 */
public class ChordAnalyzer {

    /**
     * Threshold of how much does a scale have to match given chords to be
     * included in the selection and displayed in a table.
     */
    public static final double REQUIRED_ACCURACY = 0.7;
    /**
     * Weight assigned to the root tone of a chord. Since it is the most
     * important tone of a chord, scales without this tone will get worse
     * rating.
     */
    public static final int ROOT_WEIGHT = 3;
    private static int chordCounter = 0;
    private static JFrame frame;
    private static JPanel chordsPanel;
    private static JTable scalesTable;
    private static JPanel singleTonality;
    private static List<JPanel> chordsInput = new ArrayList<>();
    private final static String GUITAR = "Kytara";
    private final static String PIANO = "Klavír";
    private static JPanel fretboard;
    private static LinkedHashMap<String, int[]> knownChords = new LinkedHashMap<>();
    private static WeightedScale actualScale = null;

    /**
     * Listener for button-triggered MIDI playing of the scale. Overrides
     * actionPerformed method to play MIDI sequence.
     */
    private static class PlayActionListener implements ActionListener {

        /**
         * Plays currently displayed scale using MIDI. Creates a {@link Timer}
         * object which serves for playing notes in a sequence. Length of each
         * note is 300 ms.
         */
        @Override
        public void actionPerformed(ActionEvent ae) {

            if (actualScale == null) {
                return;
            }
            int delay = 300; //milliseconds between notes
            try {
                Timer timer = new Timer(delay, new TimerListener());
                timer.start();
            } catch (MidiUnavailableException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Chyba zařízení MIDI. Přehrávání není možné.",
                        "Chyba zařízení MIDI",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Provides MIDI interface for playing a scale.
     */
    private static class TimerListener implements ActionListener {

        int counter = 0;
        Synthesizer synth;
        MidiChannel[] mc;
        int[] scale;

        /**
         * Constructor. Initializes an array of MIDI notes to be played from
         * current scale and establishes MIDI synthesizer.
         */
        public TimerListener() throws MidiUnavailableException {
            scale = new int[actualScale.getMask().length + 1];
            System.arraycopy(actualScale.getMask(), 0, scale, 0, actualScale.getMask().length);

            //we add one more root note one octave higher at the end of the sequence, for "complete" sound of a scale
            scale[scale.length - 1] = 12;

            synth = MidiSystem.getSynthesizer();
            synth.open();
            mc = synth.getChannels();
            Instrument[] instr = synth.getDefaultSoundbank().getInstruments();
            synth.loadInstrument(instr[0]);
        }

        /**
         * Triggered by Timer event, plays one single actual note from the array
         * of notes.
         *
         * @param ae Timer event that triggered the method
         */
        @Override
        public void actionPerformed(ActionEvent ae) {
            //MIDI note 48 is mid C
            int root = 48 + actualScale.getRoot().ordinal();

            //switch off previous note
            if (counter > 0) {
                mc[4].noteOff(root + scale[counter - 1]);
            }
            //check if there are other notes to play
            if (counter >= scale.length) {
                ((Timer) ae.getSource()).stop();
                return;
            }
            //switch on next note
            mc[4].noteOn(root + scale[counter++], 75);
        }
    }

    /**
     * Listener for changes of selection in the table of scales.
     */
    private static class ScaleSelectedListener implements ListSelectionListener {

        /**
         * Gets currently selected scales in the table and updates GUI to
         * display information about the scale.
         *
         * @param lse event that triggered the method
         */
        @Override
        public void valueChanged(ListSelectionEvent lse) {
            //to prevent from being called multiple times times for the same event
            if (lse.getValueIsAdjusting()) {
                return;
            }
            int rowSelected = scalesTable.getSelectedRow();
            if (rowSelected == -1) {
                return;
            }
            actualScale = (WeightedScale) scalesTable.getValueAt(rowSelected, 0);
            singleScaleDisplay.updateDisplayedScale();
        }
    }

    /**
     * Listener for removing chord input fields from the GUI. Performs the
     * removal itself and then modifies the last chord of the remainder to span
     * vertically over the rest of the enclosing JPanel.
     */
    private static class RemoveLastActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (chordsInput.size() > 0) {
                
                //removal from both JPanel chordsPanel AND array field chordsInput
                chordsPanel.remove(chordsInput.get(chordsInput.size() - 1));
                chordsInput.remove(chordsInput.size() - 1);
                chordCounter--;
                
                //span the last chord vertically
                if (chordsInput.size() > 0) {
                    JPanel j = chordsInput.get(chordsInput.size() - 1);
                    GridBagConstraints cc = ((GridBagLayout) chordsPanel.getLayout()).getConstraints(j);
                    //change value of vertical span from 0.0 to 1.0
                    cc.weighty = 1.0;
                    ((GridBagLayout) chordsPanel.getLayout()).setConstraints(j, cc);
                }
                chordsPanel.revalidate();
                chordsPanel.repaint();
            }
        }
    }

    /**
     * Triggers the chord analysis. Creates a
     * <code>Chord</code> array from user input, finds suitable scales, sorts
     * them descendingly by their accuracy and displays them in a table.
     */
    private static class analyzeActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            Chord[] chords = new Chord[chordCounter];
            int i = 0;
            for (JPanel panel : chordsInput) {
                chords[i++] = new Chord(
                        Tone.values()[((JComboBox) panel.getComponent(1)).getSelectedIndex()],
                        (String) ((JComboBox) panel.getComponent(2)).getSelectedItem());

            }
            //find suitable scales...
            List<WeightedScale> result = findScales(chords);
            //..sort them descendingly by accuracy...
            Collections.sort(result, Collections.reverseOrder());
            //...and display them in a table
            displayScales(result);
        }
    }

    /**
     * Helper class for encapsulating all the GUI elements serving for
     * displaying a single scale and its properties. Serves solely for easier
     * access and cleaner code.
     */
    private static class singleScaleDisplay {

        static JPanel panel = new JPanel(new GridBagLayout());
        static JLabel name = new JLabel(" ");
        static JLabel tonesLabel = new JLabel("Tóny:");
        static JLabel tones = new JLabel("");
        static JLabel maskLabel = new JLabel("Maska:");
        static JLabel mask = new JLabel("");
        static JLabel intervalsLabel = new JLabel("Intervaly:");
        static JLabel intervals = new JLabel("");
        static JLabel chordsLabel = new JLabel("Použitelné akordy:");
        static JPanel playableChordsHolder = new JPanel(new GridBagLayout());
        static JLabel[] playableChords = new JLabel[0];
        static JButton play = new JButton("Přehrát");
        static PianoPanel pianoPanel;
        static GuitarPanel guitarPanel;
        static int count = 0;

        /**
         * Updates all relevent GUI elements to display information about
         * currently selected scale. Called upon a change of selection in
         * <code>scalesTable</code>. Counts new tonal mask, intervals and
         * displays correct tones respective to
         * <code>actualScale</code>. Calls
         * <code>findSuitableChords</code> method and updates respective
         * graphics elements. Also calls update on
         * <code>PianoPanel</code> and
         * <code>GuitarPanel</code> objects with a parameter being
         * <code>actualScale</code>.
         */
        static public void updateDisplayedScale() {
            name.setText(actualScale.toString());

            StringBuilder sbTones = new StringBuilder();
            StringBuilder sbMask = new StringBuilder();
            StringBuilder sbIntervals = new StringBuilder();

            int last = 0;

            for (int i : actualScale.getMask()) {
                sbTones.append(Tone.values()[(actualScale.getRoot().ordinal() + i) % 12].toString()).append(" ");
                sbMask.append(Integer.toString(i)).append(" - ");
                sbIntervals.append(Integer.toString(i - last)).append(" - ");
                last = i;
            }
            tones.setText(sbTones.substring(0, sbTones.length() - 1)); //removing the last space
            mask.setText(sbMask.substring(0, sbMask.length() - 3)); //removing the last " - "
            intervals.setText(sbIntervals.substring(0, sbIntervals.length() - 3)); //removing the last " - "


            //Suitable chords are divided into multiple lines, one for each root tone. 
            //Number of root tones changes for each scale, so it must be recreated every time a new scale is selected.
            Chord[][] crdss = findSuitableChords(actualScale);
            playableChords = new JLabel[crdss.length];
            int i = 0;
            StringBuilder sbChords;
            for (Chord[] crds : crdss) {
                sbChords = new StringBuilder();
                for (Chord crd : crds) {
                    sbChords.append(crd.getRoot().toString()).append(crd.getName()).append(", ");
                }
                playableChords[i++] = new JLabel(sbChords.substring(0, sbChords.length() - 2)); //removing the last comma
            }

            //remove old chords
            panel.remove(playableChordsHolder);

            //add new array of JLabels to GUI
            playableChordsHolder = new JPanel(new GridBagLayout());
            i = 0;
            GridBagConstraints c;
            for (JLabel chord : playableChords) {
                c = new GridBagConstraints(0, (i++), 1, 1, 1.0, 1.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
                //  chord.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                playableChordsHolder.add(chord, c);
            }
            
            c = new GridBagConstraints(3, 1, 1, 4, 0, 1.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, new Insets(0, 10, 20, 40), 0, 0);
            panel.add(playableChordsHolder, c);

            //update visual representation of the scale on guitar fretboard and piano keyboard
            if (pianoPanel != null) {
                pianoPanel.setScale(actualScale);
            }
            if (guitarPanel != null) {
                guitarPanel.setScale(actualScale);
            }

            panel.repaint();
        }

        /**
         * Returns JPanel holding all the elements of a single scale display.
         * Arranges elements of the panel using multiple embedded GridBag
         * layouts and a CardLayout for
         * <code>PianoPanel</code> and
         * <code>GuitarPanel</code> objects. 
         * Called only once during set up of the GUI.
         *
         * @return panel holding the single scale display
         */
        static public JPanel getPanel() {
            panel.setBorder(BorderFactory.createLineBorder(Color.black, 1));

            GridBagConstraints c;

            c = new GridBagConstraints(0, 0, 3, 1, 0, 0.1,
                    GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(20, 40, 15, 10), 0, 0);
            name.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            panel.add(name, c);

            c = new GridBagConstraints(0, 1, 2, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 10, 10), 0, 0);
            play.addActionListener(new PlayActionListener());
            panel.add(play, c);

            c = new GridBagConstraints(0, 2, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 3, 15), 0, 0);
            tonesLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            panel.add(tonesLabel, c);

            c = new GridBagConstraints(1, 2, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0);
            panel.add(tones, c);

            c = new GridBagConstraints(0, 3, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 3, 15), 0, 0);
            maskLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            panel.add(maskLabel, c);

            c = new GridBagConstraints(1, 3, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0);
            panel.add(mask, c);

            c = new GridBagConstraints(0, 4, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 15, 15), 0, 0);
            intervalsLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            panel.add(intervalsLabel, c);

            c = new GridBagConstraints(1, 4, 1, 1, 0, 0.1,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(3, 0, 15, 5), 0, 0);
            panel.add(intervals, c);

            c = new GridBagConstraints(2, 1, 1, 3, 0, 1.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 0, 15), 0, 0);
            chordsLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            panel.add(chordsLabel, c);

            c = new GridBagConstraints(3, 1, 1, 4, 0, 1.0,
                    GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, new Insets(0, 10, 20, 15), 0, 0);
            panel.add(playableChordsHolder, c);

            //radioButtons for switching between guitar and piano display in a CardLayout
            ButtonGroup instruments = new ButtonGroup();
            JPanel instrumentsPanel = new JPanel(new GridBagLayout());

            c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
            JRadioButton radioPiano = new JRadioButton("Klavír");
            radioPiano.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ((CardLayout) (fretboard.getLayout())).show(fretboard, PIANO);
                }
            });
            radioPiano.setSelected(true);
            instruments.add(radioPiano);
            instrumentsPanel.add(radioPiano, c);

            c = new GridBagConstraints(0, 1, 1, 1, 0, 0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
            JRadioButton radioGuitar = new JRadioButton("Kytara");
            radioGuitar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    ((CardLayout) (fretboard.getLayout())).show(fretboard, GUITAR);
                }
            });
            instruments.add(radioGuitar);
            instrumentsPanel.add(radioGuitar, c);

            c = new GridBagConstraints(0, 5, 1, 1, 0, 1.0,
                    GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 40, 80, 0), 0, 0);
            panel.add(instrumentsPanel, c);

            //adding the guitar fretboard and piano keyboard into the CardLayout
            c = new GridBagConstraints(1, 5, 3, 2, 1.0, 1.0,
                    GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(20, 0, 20, 40), 0, 0);
            fretboard = new JPanel(new CardLayout());
            try {
                pianoPanel = new PianoPanel();
                fretboard.add(pianoPanel, PIANO);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Chyba při načítání obrázku 'piano.jpg'.",
                        "Chyba při načítání obrázku.",
                        JOptionPane.ERROR_MESSAGE);
            }
            try {
                guitarPanel = new GuitarPanel();
                fretboard.add(guitarPanel, GUITAR);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Chyba při načítání obrázku 'guitar.jpg'.",
                        "Chyba při načítání obrázku.",
                        JOptionPane.ERROR_MESSAGE);
            }

            panel.add(fretboard, c);

            return panel;
        }
    }

    /**
     * Finds all the suitable scales for given chord progression and rates them
     * according to percentual match against input chords. Checks every scale
     * from {@code scales} input file beginning at all possible roots against
     * tones from input chords. Root tones are assigned greater importance given
     * by {@code ROOT_WEIGHT}. Accuracy is then computed as number of matches *
     * weight of the tone / sum of assigned weights. If this number is bigger
     * than {@code REQUIRED_ACCURACY}, then the scale is added to result.
     *
     * @param chords an array of {@link Chord} objects for which a suitable
     * scale is to be found
     * @return a List of WeightedScale - all suitable scales ranked by
     * percentual correspondence
     */
    public static List<WeightedScale> findScales(Chord[] chords) {
        List<Scale> scales = loadScales();
        List<WeightedScale> result = new ArrayList<>();

        //weights for tones C, C# .. B
        int[] weights = new int[12];

        //sum of all asigned weights
        int sumWeights = 0;

        for (Chord c : chords) {
            int numRoot = c.getRoot().ordinal();

            if (weights[numRoot] < ROOT_WEIGHT) {
                weights[numRoot] = ROOT_WEIGHT;
            }

            for (int i : knownChords.get(c.getName())) {
                if (weights[(numRoot + i) % 12] < 1) {
                    weights[(numRoot + i) % 12]++;
                }
            }
        }
        for (int i : weights) {
            sumWeights += i;
        }

        //check every scale against every one of 12 possible beginning tones, match against weighted list and assign accuracy
        for (Scale s : scales) {
            for (int root = 0; root < 12; root++) {
                int numHits = 0;
                for (int i : s.getMask()) {
                    numHits += weights[(root + i) % 12];
                }

                //scales with accuracy bigger than a REQUIRED_ACCURACY are added to result
                double accuracy = (double) numHits / sumWeights;
                if (accuracy > REQUIRED_ACCURACY) {
                    result.add(new WeightedScale(s, Tone.values()[root], accuracy));
                }
            }
        }
        return result;
    }

    /**
     * Loads a database of known scales from the file 'scales' in application's
     * directory. Each scale is on a separate line as a scale name followed by
     * the scale mask, everything separated by colons. For example:
     * <code>melodic minor:0:2:3:5:7:9:11</code>.
     *
     * @return List of Scale objects
     */
    public static List<Scale> loadScales() {
        List<Scale> loadedScales = new ArrayList<>();
        int[] seg;
        String line;
        try {
            //charset has to be specified for Czech
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("scales")), "UTF-8"));
            while ((line = reader.readLine()) != null) {
                String[] lineparts = line.split(":");
                seg = new int[lineparts.length - 1];
                for (int i = 1; i < lineparts.length; i++) {
                    seg[i - 1] = Integer.parseInt(lineparts[i]);
                }
                loadedScales.add(new Scale(lineparts[0], seg));
            }
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,
                    "Chyba při čtení souboru 'scales'. Ujistěte se, že soubor je v adresáři programu a ve správném formátu.",
                    "Chyba při načítání databáze stupnic",
                    JOptionPane.ERROR_MESSAGE);
        }
        return loadedScales;
    }

    /**
     * Loads a database of known chords from the file 'chords' in application's
     * directory. Each chord is on a separate line as a chord name followed by
     * the chord mask, everything separated by colons. For example:
     * {@code dur:0:4:7}.
     */
    public static void loadChords() {
        int[] seg;
        String line;
        try {
            //charset has to be specified for Czech
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("chords")), "UTF-8"));
            while ((line = reader.readLine()) != null) {
                String[] lineparts = line.split(":");
                seg = new int[lineparts.length - 1];
                for (int i = 1; i < lineparts.length; i++) {
                    seg[i - 1] = Integer.parseInt(lineparts[i]);
                }
                knownChords.put(lineparts[0], seg);
            }
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,
                    "Chyba při čtení souboru 'chords'. Ujistěte se, že soubor je v adresáři programu a ve správném formátu.",
                    "Chyba při načítání databáze akordů",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Error while loading 'chords' file.");
            System.exit(1);
        }

    }

    /**
     * Finds all the chords which fits given WeightedScale. Suitable chords are
     * organized in two-dimensional array of {@link Chord} objects, where chords
     * derived from a common root note are in the same array. Algorithm tries
     * all possible chord shapes derived from all possible root tones given by
     * the scale. Scale is converted to a boolean array representation. The
     * array has a length of 12, for each note from C to B, and the value
     * <code>true</code> means that the tone is in the given scale, whereas
     * <code>false</code> means the opposite. Each chord shape derived from a
     * certain root then gives certain indices in the boolean array, which has
     * to be all true, if the chord fits the scale.
     *
     *
     * @param wscale scale for which the chords are to be found
     * @return two-dimensional array of {@link Chord} objects fitting the input
     * scale
     */
    public static Chord[][] findSuitableChords(WeightedScale wscale) {
        ArrayList<Chord[]> result = new ArrayList<>();
        boolean fits;

        //boolean mask representation of a scale
        boolean[] scaleBool = new boolean[12];
        for (int i = 0; i < wscale.getMask().length; i++) {
            scaleBool[wscale.getMask()[i]] = true;
        }


        for (int root : wscale.getMask()) {
            ArrayList<Chord> rootResult = new ArrayList<>();
            //for each possible root we try every possible chord shape from knownChords, previously loaded form the file 'chords'
            for (Map.Entry<String, int[]> c : knownChords.entrySet()) {
                fits = true;
                for (int i : c.getValue()) {
                    if (!scaleBool[(root + i) % 12]) {
                        //all the tones of a chord must fit the scale
                        fits = false;
                    }
                }
                if (fits) {
                    rootResult.add(new Chord(Tone.values()[(wscale.getRoot().ordinal() + root) % 12], c.getKey()));
                }
            }
            //empty lists are not added to the result 
            if (rootResult.size() > 0) {
                result.add(rootResult.toArray(new Chord[0]));
            }
        }
        return result.toArray(new Chord[0][0]);
    }

    /**
     * Updates GUI by adding fields for entering another chord. Simultanneously
     * sets this last added chord to span over the rest of the enclosing JPanel.
     * References to all the panels with elements for entering chords are stored
     * in the array
     * <code>chordsInput</code>.
     */
    public static void addChord() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c;

        //the last chord in list before adding the new one should no longer span vertically
        if (chordsInput.size() > 0) {
            JPanel j = chordsInput.get(chordsInput.size() - 1);
            c = ((GridBagLayout) chordsPanel.getLayout()).getConstraints(j);
            c.weighty = 0.0;
            ((GridBagLayout) chordsPanel.getLayout()).setConstraints(j, c);
        }

        //creating label
        JLabel chordLabel = new JLabel("Akord " + ++chordCounter);
        c = new GridBagConstraints(0, 0, 2, 1, 0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(15, 20, 0, 10), 0, 0);
        panel.add(chordLabel, c);

        //creating combo box of tones
        String[] toneNames = new String[12];
        int i = 0;
        for (Tone t : Tone.values()) {
            toneNames[i++] = t.toString();
        }
        JComboBox comboRoot = new JComboBox(toneNames);
        c = new GridBagConstraints(0, 1, 1, 1, 0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 15, 5, 5), 0, 0);
        panel.add(comboRoot, c);

        //creating combo box of chord types
        String[] chordTypes = knownChords.keySet().toArray(new String[0]);
        JComboBox comboType = new JComboBox(chordTypes);
        c = new GridBagConstraints(1, 1, 1, 1, 1.0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 15, 5, 5), 0, 0);
        panel.add(comboType, c);

        //adding the panel with graphic elements for entering a new chord into the application layout
        c = new GridBagConstraints(0, chordCounter, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        chordsPanel.add(panel, c);

        chordsPanel.revalidate();
        
        //adding new chord panel to chordsInput array
        chordsInput.add(panel);
    }

    /**
     * Updates the table model of scalesTable to reflect current list of
     * suitable scales. Method creates new
     * <code>DefaulTableModel</code> and fills it with actual data, represented
     * as two-dimensional array of
     * <code>Object</code>. First column contains the
     * <code>WeightedScale</code> objects, displayed using redefined
     * <code>toString</code> method. Second column contains percentual accuracy
     * of a scale in a text format. This new model then replaces the old one in
     * <code>JTable</code> object
     * <code>scalesTable</code>.
     *
     * @param wscales current list of scales to be displayed
     */
    public static void displayScales(final List<WeightedScale> wscales) {
        Object[][] scaleData = new Object[wscales.size()][2];

        int i = 0;
        for (WeightedScale w : wscales) {
            scaleData[i][0] = w;
            scaleData[i++][1] = Integer.toString(w.getAccuracy()) + " %";
        }
        final String[] columnNames = new String[]{"Tónina", "Procentuální shoda"};

        DefaultTableModel model = new DefaultTableModel(scaleData, columnNames) {
            @Override
            public boolean isCellEditable(int i, int i1) {
                return false;
            }
        };
        scalesTable.setModel(model);
        scalesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scalesTable.getSelectionModel().addListSelectionListener(new ScaleSelectedListener());
    }

    /**
     * Method for creating GUI. Sets up all graphics elements of the application
     * using multiple embedded GridBag layouts. Links buttons with their
     * respective listeners. Note: bottom panel for displaying detailed
     * information about selected scale is set up using
     * <code>getPanel</code> function of holder class
     * <code>singleScaleDisplay</code>.
     */
    public static void createAndShowGUI() {
        frame = new JFrame("Analyzér akordů");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //set up chords panel
        chordsPanel = new JPanel(new GridBagLayout());
        //fields for entering one chord are prepared, more can be added by user
        addChord();
        JScrollPane chordsScroll = new JScrollPane(chordsPanel);
        chordsScroll.setMinimumSize(new Dimension(210, 250));
        chordsScroll.setPreferredSize(new Dimension(210, 250));
        chordsScroll.setBorder(BorderFactory.createLineBorder(Color.black, 1));

        //set up buttons for adding/removing chords and analysis
        JButton addChordButton = new JButton("Přidat");
        addChordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addChord();
            }
        });
        JButton removeChordButton = new JButton("Odebrat poslední");
        removeChordButton.addActionListener(new RemoveLastActionListener());
        JButton analyzeButton = new JButton("Analyzovat");
        analyzeButton.addActionListener(new analyzeActionListener());

        //set up scales panel
        scalesTable = new JTable();
        scalesTable.setFillsViewportHeight(true);
        JScrollPane scalesScroll = new JScrollPane(scalesTable);
        scalesScroll.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        scalesScroll.setPreferredSize(scalesScroll.getMinimumSize());

        //set up single scale view
        singleTonality = singleScaleDisplay.getPanel();

        //add all elements to frame layout
        Container cont = frame.getContentPane();
        cont.setLayout(new GridBagLayout());
        GridBagConstraints c;
        c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0);
        cont.add(chordsScroll, c);

        c = new GridBagConstraints(0, 1, 1, 1, 0, 0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10, 10, 0, 10), 0, 0);
        cont.add(addChordButton, c);

        c = new GridBagConstraints(0, 2, 1, 1, 0, 0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 10, 10, 10), 0, 0);
        cont.add(removeChordButton, c);

        c = new GridBagConstraints(0, 3, 1, 1, 0, 0,
                GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 10, 10, 10), 0, 0);
        cont.add(analyzeButton, c);

        c = new GridBagConstraints(1, 0, 1, 4, 0, 0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0);
        cont.add(scalesScroll, c);

        c = new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0);
        cont.add(singleTonality, c);

        frame.pack();
        
        //keeps window from shrinking below level given by minimalSize of all components
        frame.setMinimumSize(new Dimension(frame.getMinimumSize().width, frame.getMinimumSize().height + 40)); 
        
        frame.setVisible(true);
    }

    /**
     * Main function. Loads list of known chords from file "chords" located in
     * the application directory, then invokes new thread for displaying GUI.
     */
    public static void main(String[] args) {
        loadChords();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
