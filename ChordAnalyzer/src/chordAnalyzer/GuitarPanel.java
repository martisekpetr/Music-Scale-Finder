package chordAnalyzer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Customized {@link JPanel} for displaying guitar fretboard with notes marked accordingly to currently selected scale. Redefines the <code>paintComponent</code> method
 * to paint a guitar fretboard from the file <code>guitar.jpg</code> on the background and graphic representation of a given scale on the front. A scale is represented
 * as a set of color circles on a fretboard with names of tones and the root tone distinguished by a different color.
 * 
 * @see JPanel
 */
public class GuitarPanel extends JPanel {

    private BufferedImage _image;
    private WeightedScale _scale;

    /**
     * Constructor. Loads image 'guitar.jpg' which is painted on the panel and
     * sets dimensions of the panel to hold the image.
     *
     * @throws IOException
     */
    public GuitarPanel() throws IOException {
        _image = ImageIO.read(new File("guitar.jpg"));

        this.setPreferredSize(new Dimension(_image.getWidth(), _image.getHeight()));
        this.setMinimumSize(new Dimension(_image.getWidth(), _image.getHeight()));
    }

    /**
     * Updates the panel to display currently selected scale.
     *
     * @param wscale currently selected scale
     */
    public void setScale(WeightedScale wscale) {
        _scale = wscale;
        this.repaint();
    }

    /**
     * Paints specific tone in all his occurencies on a fretboard.
     *
     * @param t tone to be painted
     * @param g Graphics object to be drawn to
     */
    private void paintTone(Tone t, Graphics g) {
        //strings in standard tuning
        Tone[] openStrings = new Tone[]{Tone.E, Tone.B, Tone.G, Tone.D, Tone.A, Tone.E};
        
        //frets are narrowing in the direction of a bridge, this array maps number of a fret to a x-coordinate
        int[] fretMap = new int[]{20, 75, 145, 212, 280, 345, 405, 463, 517, 568, 615, 661, 705};
        
        for (int j = 0; j < openStrings.length; j++) {
            //painting only first octave (first twelve frets on the fretboard)
            for (int i = 0; i < 13; i++) {
                if (t.equals(Tone.values()[(openStrings[j].ordinal() + i) % 12])) {
                    g.setColor(new Color(255, 255, 100, 255));
                    if (t.equals(_scale.getRoot())) {
                        //root is distinguished with different color
                        g.setColor(new Color(250, 150, 50, 255));
                    }
                    //circle is drawn to mark a tone
                    g.fillOval(fretMap[i], 28 * j, 23, 23);
                    g.setColor(Color.black);
                    g.drawOval(fretMap[i], 28 * j, 23, 23);
                    
                    //tone name is drawn over the circle
                    g.setColor(Color.black);
                    g.setFont(new Font("arial", Font.BOLD, 14));
                    
                    //"center align"
                    g.drawString(t.toString(), fretMap[i] + 9 - 3 * t.toString().length(), 28 * j + 17);
                }
            }
        }
    }

    /**
     * Paints component itself, guitar fretboard from image 'guitar.jpg' and
     * marks all the tones from currently displayed scale.
     *
     * @param g Graphics object to be drawn to.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(_image, 0, 0, null);
        if (_scale != null) {
            for (Tone t : _scale.getTones()) {
                paintTone(t, g);
            }
        }
    }
}