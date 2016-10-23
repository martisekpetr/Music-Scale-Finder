/**
 * Provides classes for runing a window application <b>ChordAnalyzer</b>, which
 * takes an arbitrary number of chords on input, finds all suitable scales
 * matching this given chord progression and displays detailed musical
 * information about each scale including other chords that fit the
 * scale.
 * <br/>
 *
 * The class
 * <code>ChordAnalyzer</code> contains all the graphic 
 * <code>swing</code> elements and provides most of the functionality. The other
 * classes are either custom structures representing musical objects (
 * <code>Chord, Scale, WeightedScale, Tone</code>) or redefined
 * <code>swing</code> objects (
 * <code>GuitarPanel, PianoPanel</code>).
 * <br/>
 *
 * <h3>The algorithm</h3>
 * The basic idea is to extract single tones from the chords on the input and
 * then compare them to all known scales, one by one, looking for the best
 * match. It is necessary to enumerate the known chords AND the known scales
 * beforehand, so that the application has a database to work with. This is done
 * in files 
 * <code>chords</code> and
 * <code>scales</code> respectively. The first one provides the user a wide
 * variety of known chord types, specified by a name, to choose from; while the
 * second file gives the algorithm a list of scales to try.
 * <h4>Example</h4>
 * User inserted these chords: <i>Cdur, Dmi</i> and <i>Fmaj</i>. Tonal masks specified in the file <code>chords</code> are:
 * <ol>
 * <li><code>dur:0:4:7</code></li>
 * <li><code>mi:0:3:7</code></li>
 * <li><code>maj:0:4:7:11</code></li>
 * </ol>
 * <p>Therefore, for Cdur we take tones 0 semitones above C, 4 semitones above C and 7 semitones above C, which gives us tones C, E and G. Anagolously, for Dmi we get 
 * D, F and A and finally for Fmaj tones F, A, C, E. Union of these sets contains tones C, D, E, F, G and A. Tones C, D and F are roots in given chords, which makes them more important,
 * therefore they are assigned weight specified in the field <code>ROOT_WEIGHT</code> (default value is 3). Rest of the tones are given weight 1.
 * </p>
 * <p>
 * We will further work with an array of weights for all 12 musical tones (that is C, C#, D, D#, E, F, F#, G, G#, A, Bb, B), which in our case looks like this:
 * <code>{3,0,3,0,1,3,0,1,0,1,0,0}</code>. Now the algorithm will try all scales in the file <code>scales</code>, one by one, to fit this mask, with root tone of the scale being
 * successively each of the twelve musical tones. That means it will try scales C major, C# major, D major, ... , B major; C minor, C# minor, ... , B minor; C harmonic minor, 
 * C# harmonic minor and so on. Each scale is again given by a mask, for instance major scale has a mask <code>0:2:4:5:7:9:11</code>. This mask, shifted by a specific root tone,
 * will give indices into our array of weights. For example F major will give indices <code>({0,2,4,5,7,9,11} + 5) mod 12 = {5,7,9,10,0,2,4}</code>. We will count all weights given
 * by these indices and then divide by a total number of weights assigned. If the resulting value (= scale accuracy) is equal to 1, then all the tones in given chords are in the scale (so tones 
 * in Cdur, Dmi and Fmaj are all in an F major scale). Value lesser than 1 indicates that some of the tones are missing in the scale. Notice that if a root tone (with the weight 3) 
 * is missing, the accuracy will be decreased noticeably. Scales with the accuracy greater than a value given in a field REQUIRED_ACCURACY are displayed on the output.
 * 
 * </p>
 */
package chordAnalyzer;